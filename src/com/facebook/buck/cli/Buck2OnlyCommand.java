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

package com.facebook.buck.cli;

import com.facebook.buck.event.ConsoleEvent;
import com.facebook.buck.util.ExitCode;

/** {@code buck build} A placeholder command for commands in buck2 that's not in buck1. */
public abstract class Buck2OnlyCommand extends AbstractCommand {

  @Override
  public ExitCode runWithoutHelp(CommandRunnerParams params) throws Exception {
    params
        .getBuckEventBus()
        .post(
            ConsoleEvent.severe(
                String.format("The command `%s` is only available on buck2", getName())));

    return ExitCode.COMMANDLINE_ERROR;
  }

  /** @return the name of this command, as is, for the command line */
  public abstract String getName();

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public String getShortDescription() {
    return null;
  }
}