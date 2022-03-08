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

package com.facebook.buck.jvm.java;

import com.facebook.buck.core.cell.name.CanonicalCellName;
import com.facebook.buck.core.filesystems.AbsPath;
import com.facebook.buck.core.filesystems.RelPath;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.Map;

public interface ClassUsageFileWriter {

  void writeFile(
      ImmutableMap<Path, Map<Path, Integer>> classUsageMap,
      RelPath relativePath,
      AbsPath rootPath,
      RelPath configuredBuckOut,
      ImmutableMap<CanonicalCellName, RelPath> cellToPathMappings);
}
