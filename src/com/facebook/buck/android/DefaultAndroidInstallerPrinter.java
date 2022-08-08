/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.buck.android;

import com.facebook.buck.core.build.execution.context.ExecutionContext;
import com.facebook.buck.event.BuckEventBus;
import com.facebook.buck.event.ConsoleEvent;
import com.facebook.buck.util.Ansi;
import java.util.function.Supplier;

/** Android logger for buckv1 installs */
public class DefaultAndroidInstallerPrinter implements AndroidInstallPrinter {
  private final Supplier<ExecutionContext> contextSupplier;

  public DefaultAndroidInstallerPrinter(Supplier<ExecutionContext> contextSupplier) {
    this.contextSupplier = contextSupplier;
  }

  @Override
  public void printMessage(String message) {
    getBuckEventBus().post(ConsoleEvent.info(message));
  }

  @Override
  public void printSuccess(String successMessage) {
    Ansi ansi = contextSupplier.get().getAnsi();
    getBuckEventBus().post(ConsoleEvent.info(ansi.asHighlightedSuccessText(successMessage)));
  }

  @Override
  public void printError(String failureMessage) {
    getBuckEventBus().post(ConsoleEvent.severe(failureMessage));
  }

  private BuckEventBus getBuckEventBus() {
    return contextSupplier.get().getBuckEventBus();
  }
}