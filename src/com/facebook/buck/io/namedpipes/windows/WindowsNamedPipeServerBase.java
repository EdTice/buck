/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.buck.io.namedpipes.windows;

import static com.facebook.buck.io.namedpipes.windows.WindowsNamedPipeLibrary.closeConnectedPipe;
import static com.facebook.buck.io.namedpipes.windows.WindowsNamedPipeLibrary.createEvent;

import com.facebook.buck.io.namedpipes.BaseNamedPipe;
import com.facebook.buck.io.namedpipes.NamedPipeReader;
import com.facebook.buck.io.namedpipes.NamedPipeServer;
import com.facebook.buck.io.namedpipes.NamedPipeWriter;
import com.facebook.buck.io.namedpipes.PipeNotConnectedException;
import com.facebook.buck.io.namedpipes.windows.handle.WindowsHandle;
import com.facebook.buck.io.namedpipes.windows.handle.WindowsHandleFactory;
import com.facebook.buck.util.CloseableWrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Implements a {@link com.facebook.buck.io.namedpipes.NamedPipe} backed by a windows named pipe
 * under the hood.
 *
 * <p>Ported from {@link com.facebook.nailgun.NGWin32NamedPipeServerSocket}
 */
abstract class WindowsNamedPipeServerBase extends BaseNamedPipe implements NamedPipeServer {

  private static final WindowsNamedPipeLibrary API = WindowsNamedPipeLibrary.INSTANCE;

  private static final int KB_IN_BYTES = 1024;
  // https://docs.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-createnamedpipea
  // The buffer size specified should be small enough that your process will not run out of nonpaged
  // pool, but large enough to accommodate typical requests.
  private static final int BUFFER_SIZE = 2 * KB_IN_BYTES;

  private static final int WAIT_FOR_HANDLER_TIMEOUT_MILLIS = 5_000;

  private final LinkedBlockingQueue<WindowsHandle> openHandles = new LinkedBlockingQueue<>();
  private final LinkedBlockingQueue<WindowsHandle> connectedHandles = new LinkedBlockingQueue<>();
  private final Consumer<WindowsHandle> closeCallback;
  private final WindowsHandleFactory windowsHandleFactory;
  private boolean isClosed = false;

  public WindowsNamedPipeServerBase(Path path, WindowsHandleFactory windowsHandleFactory) {
    super(path);
    this.windowsHandleFactory = windowsHandleFactory;
    this.closeCallback =
        handle -> {
          if (connectedHandles.remove(handle)) {
            closeConnectedPipe(handle, false);
          }
          if (openHandles.remove(handle)) {
            closeOpenPipe(handle);
          }
        };
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }

  protected <T extends WindowsNamedPipeClientBase> T connect(Class<T> clazz) throws IOException {
    String namedPipe = getName();
    WindowsHandle handle =
        windowsHandleFactory.create(
            API.CreateNamedPipe(
                /* lpName */ namedPipe,
                /* dwOpenMode */ WinNT.PIPE_ACCESS_DUPLEX | WinNT.FILE_FLAG_OVERLAPPED,
                /* dwPipeMode */ WinBase.PIPE_REJECT_REMOTE_CLIENTS,
                /* nMaxInstances */ WinBase.PIPE_UNLIMITED_INSTANCES,
                /* nOutBufferSize */ BUFFER_SIZE,
                /* nInBufferSize */ BUFFER_SIZE,
                /* nDefaultTimeOut */ 0,
                /* lpSecurityAttributes */ null),
            "CreateNamedPipe() for " + namedPipe);
    if (handle.isInvalidHandle()) {
      throw new IOException(
          String.format(
              "Could not create named pipe: %s, error %s",
              namedPipe, Kernel32Util.getLastErrorMessage()));
    }
    openHandles.add(handle);

    int connectError;
    try (CloseableWrapper<WindowsOverlapped> closeableWrapper =
        CloseableWrapper.of(createOverlapped(), WindowsOverlapped::close)) {
      HANDLE rawHandle = handle.getHandle();
      WindowsOverlapped overlapped = closeableWrapper.get();
      if (API.ConnectNamedPipe(rawHandle, overlapped.getPointer())) {
        openHandles.remove(handle);
        connectedHandles.add(handle);
        return getClient(handle, clazz);
      }

      connectError = Kernel32.INSTANCE.GetLastError();
      if (connectError == WinError.ERROR_PIPE_CONNECTED) {
        openHandles.remove(handle);
        connectedHandles.add(handle);
        return getClient(handle, clazz);
      }

      if (connectError == WinError.ERROR_NO_DATA) {
        // Client has connected and disconnected between CreateNamedPipe() and ConnectNamedPipe()
        // connection is broken, but it is returned it avoid loop here.
        // Actual error will happen when it will try to read/write from/to pipe.
        return getClient(handle, clazz);
      }

      if (connectError == WinError.ERROR_IO_PENDING) {
        if (!API.GetOverlappedResult(
            rawHandle, overlapped.getPointer(), new IntByReference(), true)) {
          openHandles.remove(handle);
          closeOpenPipe(handle);
          throw new PipeNotConnectedException(
              String.format(
                  "GetOverlappedResult() failed for connect operation. Named pipe: %s, error: %s, previous error: %s",
                  namedPipe,
                  Kernel32Util.getLastErrorMessage(),
                  Kernel32Util.formatMessageFromLastErrorCode(connectError)));
        }

        openHandles.remove(handle);
        connectedHandles.add(handle);
        return getClient(handle, clazz);
      }
    }

    throw new IOException(
        String.format(
            "ConnectNamedPipe() failed. Named pipe: %s, error: %s", namedPipe, connectError));
  }

  private <T extends WindowsNamedPipeClientBase> T getClient(WindowsHandle handle, Class<T> clazz) {
    if (NamedPipeWriter.class.isAssignableFrom(clazz)) {
      return clazz.cast(
          new WindowsNamedPipeClientWriter(getPath(), handle, closeCallback, windowsHandleFactory));
    }

    if (NamedPipeReader.class.isAssignableFrom(clazz)) {
      return clazz.cast(
          new WindowsNamedPipeClientReader(getPath(), handle, closeCallback, windowsHandleFactory));
    }

    throw new IllegalStateException(clazz + " is not supported!");
  }

  private WindowsOverlapped createOverlapped() throws IOException {
    WindowsHandle windowsHandle = createEvent("Overlapped for " + getName(), windowsHandleFactory);
    return new WindowsOverlapped(windowsHandle);
  }

  /**
   * Prepares to close this named pipe by flushing the named pipe if necessary, then disconnecting
   * it.
   *
   * <p>Flushing is only necessary if a client has connected. If no client connected, nothing has
   * been written to the named pipe. It is unnecessary to flush in this case, so we proceed with
   * disconnecting directly.
   *
   * <p>If a client has connecting, we should flush the named pipe to make sure that the reader gets
   * a chance to read everything that was written. After flushing, we must wait for the given {@link
   * Future} to complete before disconnecting the named pipe. Otherwise, there may be unread events
   * left in the named pipe. Those events are discarded upon disconnection and cannot be read again
   * even upon reconnection.
   */
  @Override
  public void prepareToClose(Future<Void> readyToClose)
      throws InterruptedException, ExecutionException, TimeoutException {
    try {
      if (connectedHandles.isEmpty()) {
        if (!openHandles.isEmpty()) {
          // Client never connected. There should be an open handle
          WindowsHandle handle = getTheOnlyHandle(openHandles);
          closeConnectedPipe(handle, false);
        }
      } else {
        WindowsHandle handle = getTheOnlyHandle(connectedHandles);
        API.FlushFileBuffers(handle.getHandle());

        try {
          // After flushing, we need to wait until the handler thread finishes reading everything
          // before disconnecting.
          readyToClose.get(WAIT_FOR_HANDLER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } finally {
          closeConnectedPipe(handle, true);
        }
      }
    } catch (RuntimeException e) {
      throw new ExecutionException("Unexpected runtime exception while preparing to close", e);
    }
  }

  private WindowsHandle getTheOnlyHandle(BlockingQueue<WindowsHandle> handles) {
    List<WindowsHandle> drainList = new ArrayList<>(1);
    handles.drainTo(drainList);
    int size = drainList.size();
    Preconditions.checkState(size == 1, "Expected one handle, got %s", size);
    return Iterables.getOnlyElement(drainList);
  }

  @Override
  public void close() {
    List<WindowsHandle> handlesToClose = new ArrayList<>();
    openHandles.drainTo(handlesToClose);
    for (WindowsHandle windowsHandle : handlesToClose) {
      closeOpenPipe(windowsHandle);
    }

    List<WindowsHandle> handlesToDisconnect = new ArrayList<>();
    connectedHandles.drainTo(handlesToDisconnect);
    handlesToDisconnect.forEach(handle -> closeConnectedPipe(handle, true));

    isClosed = true;
  }

  private void closeOpenPipe(WindowsHandle handle) {
    API.CancelIoEx(handle.getHandle(), null);
    handle.close();
  }
}
