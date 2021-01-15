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

package com.facebook.buck.apple;

import com.facebook.buck.core.build.execution.context.StepExecutionContext;
import com.facebook.buck.core.filesystems.AbsPath;
import com.facebook.buck.core.filesystems.RelPath;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.step.AbstractExecutionStep;
import com.facebook.buck.step.StepExecutionResult;
import com.facebook.buck.step.StepExecutionResults;
import com.facebook.buck.util.json.ObjectMappers;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/** Step to read from disk hashes of all the bundle parts of previous build. */
public class AppleBundleContentHashesReadStep extends AbstractExecutionStep {

  private ProjectFilesystem filesystem;
  private AbsPath hashesFilePath;
  private ImmutableMap.Builder<RelPath, String> pathToHashBuilder;

  AppleBundleContentHashesReadStep(
      ProjectFilesystem filesystem,
      AbsPath hashesFilePath,
      ImmutableMap.Builder<RelPath, String> pathToHashBuilder) {
    super("apple-bundle-content-hashes-read");
    this.filesystem = filesystem;
    this.hashesFilePath = hashesFilePath;
    this.pathToHashBuilder = pathToHashBuilder;
  }

  @Override
  public StepExecutionResult execute(StepExecutionContext context)
      throws IOException, InterruptedException {
    pathToHashBuilder.putAll(readContentHashesIfFileExists(filesystem, hashesFilePath));
    return StepExecutionResults.SUCCESS;
  }

  private static ImmutableMap<RelPath, String> readContentHashesIfFileExists(
      ProjectFilesystem filesystem, AbsPath hashesFilePath) throws IOException {
    ImmutableMap.Builder<RelPath, String> pathToHashBuilder = ImmutableMap.builder();
    if (filesystem.exists(hashesFilePath.getPath())) {
      JsonParser parser = ObjectMappers.createParser(hashesFilePath.getPath());
      Map<String, String> pathToHash =
          parser.readValueAs(new TypeReference<TreeMap<String, String>>() {});
      pathToHash.forEach(
          (path, hash) -> {
            pathToHashBuilder.put(RelPath.get(path), hash);
          });
    }
    return pathToHashBuilder.build();
  }
}