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

package com.facebook.buck.features.apple.projectV2;

import com.facebook.buck.apple.AppleBuildRules;
import com.facebook.buck.apple.AppleConfig;
import com.facebook.buck.apple.AppleDependenciesCache;
import com.facebook.buck.apple.AppleDescriptions;
import com.facebook.buck.apple.AppleHeaderVisibilities;
import com.facebook.buck.apple.AppleLibraryDescription;
import com.facebook.buck.apple.AppleNativeTargetDescriptionArg;
import com.facebook.buck.apple.XCodeDescriptions;
import com.facebook.buck.apple.clang.HeaderMap;
import com.facebook.buck.core.cell.Cell;
import com.facebook.buck.core.description.arg.HasTests;
import com.facebook.buck.core.filesystems.AbsPath;
import com.facebook.buck.core.filesystems.PathWrapper;
import com.facebook.buck.core.filesystems.RelPath;
import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.Flavor;
import com.facebook.buck.core.model.impl.BuildTargetPaths;
import com.facebook.buck.core.model.targetgraph.TargetGraph;
import com.facebook.buck.core.model.targetgraph.TargetNode;
import com.facebook.buck.core.model.targetgraph.impl.TargetNodes;
import com.facebook.buck.core.rules.ActionGraphBuilder;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolverAdapter;
import com.facebook.buck.core.util.log.Logger;
import com.facebook.buck.cxx.CxxDescriptionEnhancer;
import com.facebook.buck.cxx.CxxLibraryDescription;
import com.facebook.buck.cxx.CxxPreprocessables;
import com.facebook.buck.cxx.config.CxxBuckConfig;
import com.facebook.buck.cxx.toolchain.CxxPlatform;
import com.facebook.buck.cxx.toolchain.HeaderMode;
import com.facebook.buck.cxx.toolchain.HeaderVisibility;
import com.facebook.buck.features.halide.HalideCompile;
import com.facebook.buck.features.halide.HalideLibraryDescription;
import com.facebook.buck.features.halide.HalideLibraryDescriptionArg;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.rules.coercer.SourceSortedSet;
import com.facebook.buck.rules.keys.config.RuleKeyConfiguration;
import com.facebook.buck.util.types.Pair;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/** Helper class to derive and generate all settings for file headers and where to find them. */
class HeaderSearchPaths {

  private static final Logger LOG = Logger.get(HeaderSearchPaths.class);

  private final Cell projectCell;
  private final CxxBuckConfig cxxBuckConfig;
  private final CxxPlatform cxxPlatform;
  private final RuleKeyConfiguration ruleKeyConfiguration;
  private final XCodeDescriptions xcodeDescriptions;
  private final TargetGraph targetGraph;
  private final ActionGraphBuilder actionGraphBuilder;
  private final AppleDependenciesCache dependenciesCache;
  private final ProjectSourcePathResolver projectSourcePathResolver;
  private final PathRelativizer pathRelativizer;
  private final SwiftAttributeParser swiftAttributeParser;
  private final AppleConfig appleConfig;

  private final ProjectFilesystem projectFilesystem;

  HeaderSearchPaths(
      Cell projectCell,
      CxxBuckConfig cxxBuckConfig,
      CxxPlatform cxxPlatform,
      RuleKeyConfiguration ruleKeyConfiguration,
      XCodeDescriptions xcodeDescriptions,
      TargetGraph targetGraph,
      ActionGraphBuilder actionGraphBuilder,
      AppleDependenciesCache dependenciesCache,
      ProjectSourcePathResolver projectSourcePathResolver,
      PathRelativizer pathRelativizer,
      SwiftAttributeParser swiftAttributeParser,
      AppleConfig appleConfig) {
    this.projectCell = projectCell;
    this.cxxBuckConfig = cxxBuckConfig;
    this.cxxPlatform = cxxPlatform;
    this.ruleKeyConfiguration = ruleKeyConfiguration;
    this.xcodeDescriptions = xcodeDescriptions;
    this.targetGraph = targetGraph;
    this.actionGraphBuilder = actionGraphBuilder;
    this.dependenciesCache = dependenciesCache;
    this.projectSourcePathResolver = projectSourcePathResolver;
    this.pathRelativizer = pathRelativizer;
    this.swiftAttributeParser = swiftAttributeParser;
    this.appleConfig = appleConfig;

    this.projectFilesystem = projectCell.getFilesystem();
  }

  /** Derives header search path attributes for the {@code targetNode}. */
  HeaderSearchPathAttributes getHeaderSearchPathAttributes(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode) {
    ImmutableHeaderSearchPathAttributes.Builder builder =
        ImmutableHeaderSearchPathAttributes.builder().setTargetNode(targetNode);

    ImmutableSortedMap<Path, SourcePath> publicCxxHeaders = getPublicCxxHeaders(targetNode);
    builder.setPublicCxxHeaders(publicCxxHeaders);

    ImmutableSortedMap<Path, SourcePath> privateCxxHeaders = getPrivateCxxHeaders(targetNode);
    builder.setPrivateCxxHeaders(privateCxxHeaders);

    ImmutableSet<AbsPath> recursivePublicSystemIncludeDirectories =
        collectRecursivePublicSystemIncludeDirectories(targetNode);
    builder.setRecursivePublicSystemIncludeDirectories(
        recursivePublicSystemIncludeDirectories.stream()
            .map(PathWrapper::getPath)
            .collect(ImmutableSet.toImmutableSet()));

    ImmutableSet<AbsPath> recursivePublicIncludeDirectories =
        collectRecursivePublicIncludeDirectories(targetNode);
    builder.setRecursivePublicIncludeDirectories(
        recursivePublicIncludeDirectories.stream()
            .map(PathWrapper::getPath)
            .collect(ImmutableSet.toImmutableSet()));

    ImmutableSet<AbsPath> includeDirectories = extractIncludeDirectories(targetNode);
    builder.setIncludeDirectories(
        includeDirectories.stream()
            .map(PathWrapper::getPath)
            .collect(ImmutableSet.toImmutableSet()));

    ImmutableSet<Path> recursiveHeaderSearchPaths = collectRecursiveHeaderSearchPaths(targetNode);
    builder.setRecursiveHeaderSearchPaths(recursiveHeaderSearchPaths);

    ImmutableSet<Path> swiftIncludePaths = collectRecursiveSwiftIncludePaths(targetNode);
    builder.setSwiftIncludePaths(swiftIncludePaths);

    return builder.build();
  }

  /** Generates the merged header map and writes it to the public header symlink tree location. */
  ImmutableList<SourcePath> createMergedHeaderMap(ImmutableSet<BuildTarget> targets)
      throws IOException {
    HeaderMap.Builder headerMapBuilder = new HeaderMap.Builder();
    ImmutableList.Builder<SourcePath> sourcePathsToBuildBuilder = ImmutableList.builder();

    Set<TargetNode<? extends CxxLibraryDescription.CommonArg>> processedNodes = new HashSet<>();

    for (TargetNode<?> targetNode : targetGraph.getAll(targets)) {
      // Includes the public headers of the dependencies in the merged header map.
      NodeHelper.getAppleNativeNode(targetGraph, targetNode)
          .ifPresent(
              argTargetNode ->
                  visitRecursiveHeaderSymlinkTrees(
                      argTargetNode,
                      (depNativeNode, headerVisibility) -> {
                        // Skip nodes we've already processed and headers that are not public
                        if (processedNodes.contains(depNativeNode)
                            || headerVisibility != HeaderVisibility.PUBLIC) {
                          return;
                        }
                        addToMergedHeaderMap(
                            depNativeNode, headerMapBuilder, sourcePathsToBuildBuilder);
                        processedNodes.add(depNativeNode);
                      }));
    }

    // Write the resulting header map.
    Path mergedHeaderMapRoot = getPathToMergedHeaderMap();
    Path headerMapLocation = getHeaderMapLocationFromSymlinkTreeRoot(mergedHeaderMapRoot);
    projectFilesystem.mkdirs(mergedHeaderMapRoot);
    projectFilesystem.writeBytesToPath(headerMapBuilder.build().getBytes(), headerMapLocation);

    return sourcePathsToBuildBuilder.build();
  }

  /**
   * Create header symlink trees for the {@link HeaderSearchPathAttributes#targetNode()} and any
   * required header maps or generated umbrella headers for private headers. Populates {@param
   * headerSymlinkTreesBuilder} with any generated header symlink paths.
   *
   * @return Source paths that need to be build for the {@link
   *     HeaderSearchPathAttributes#targetNode()}.
   */
  ImmutableList<SourcePath> createHeaderSearchPaths(
      HeaderSearchPathAttributes headerSearchPathAttributes,
      ImmutableList.Builder<Path> headerSymlinkTreesBuilder) {
    ImmutableList.Builder<SourcePath> sourcePathsToBuildBuilder = ImmutableList.builder();

    NodeHelper.getAppleNativeNode(targetGraph, headerSearchPathAttributes.targetNode())
        .ifPresent(
            argTargetNode ->
                visitRecursiveHeaderSymlinkTrees(
                    argTargetNode,
                    (depNativeNode, headerVisibility) -> {
                      try {
                        // Skip nodes that are not public or do not ask for symlinks
                        if (headerVisibility == HeaderVisibility.PUBLIC
                            && depNativeNode
                                .getConstructorArg()
                                .getXcodePublicHeadersSymlinks()
                                .orElse(cxxBuckConfig.getPublicHeadersSymlinksEnabled())) {
                          createPublicHeaderSymlinkTree(
                              depNativeNode, getPublicCxxHeaders(depNativeNode));
                        } else if (headerVisibility == HeaderVisibility.PRIVATE) {
                          createPrivateHeaderSymlinkTree(
                              depNativeNode,
                              getPrivateCxxHeaders(depNativeNode),
                              sourcePathsToBuildBuilder,
                              depNativeNode
                                  .getConstructorArg()
                                  .getXcodePrivateHeadersSymlinks()
                                  .orElse(cxxBuckConfig.getPrivateHeadersSymlinksEnabled()),
                              headerSymlinkTreesBuilder);
                        }
                      } catch (IOException e) {
                        LOG.verbose(
                            "Failed to create public header symlink tree for target "
                                + depNativeNode.getBuildTarget().getFullyQualifiedName());
                        return;
                      }
                    }));

    return sourcePathsToBuildBuilder.build();
  }

  void visitRecursivePrivateHeaderSymlinkTreesForTests(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode,
      BiConsumer<TargetNode<? extends CxxLibraryDescription.CommonArg>, HeaderVisibility> visitor) {
    // Visits headers of source under tests.
    ImmutableSet<TargetNode<?>> directDependencies =
        ImmutableSet.copyOf(targetGraph.getAll(targetNode.getBuildDeps()));
    for (TargetNode<?> dependency : directDependencies) {
      Optional<TargetNode<CxxLibraryDescription.CommonArg>> nativeNode =
          NodeHelper.getAppleNativeNode(targetGraph, dependency);
      if (nativeNode.isPresent() && isSourceUnderTest(nativeNode.get(), dependency, targetNode)) {
        visitor.accept(nativeNode.get(), HeaderVisibility.PRIVATE);
      }
    }
  }

  static Path getObjcModulemapVFSOverlayLocationFromSymlinkTreeRoot(Path headerSymlinkTreeRoot) {
    return headerSymlinkTreeRoot.resolve("objc-module-overlay.yaml");
  }

  private ImmutableSortedMap<Path, SourcePath> getPublicCxxHeaders(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode) {
    CxxLibraryDescription.CommonArg arg = targetNode.getConstructorArg();
    if (arg instanceof AppleNativeTargetDescriptionArg) {
      Path headerPathPrefix =
          AppleDescriptions.getHeaderPathPrefix(
              (AppleNativeTargetDescriptionArg) arg, targetNode.getBuildTarget());

      ImmutableSortedMap.Builder<String, SourcePath> exportedHeadersBuilder =
          ImmutableSortedMap.naturalOrder();
      exportedHeadersBuilder.putAll(
          AppleDescriptions.convertHeadersToPublicCxxHeaders(
              targetNode.getBuildTarget(),
              projectSourcePathResolver::resolveSourcePath,
              headerPathPrefix,
              arg.getExportedHeaders()));

      for (Pair<Pattern, SourceSortedSet> patternMatchedHeader :
          arg.getExportedPlatformHeaders().getPatternsAndValues()) {
        exportedHeadersBuilder.putAll(
            AppleDescriptions.convertHeadersToPublicCxxHeaders(
                targetNode.getBuildTarget(),
                projectSourcePathResolver::resolveSourcePath,
                headerPathPrefix,
                patternMatchedHeader.getSecond()));
      }

      ImmutableSortedMap<String, SourcePath> fullExportedHeaders = exportedHeadersBuilder.build();
      return convertMapKeysToPaths(fullExportedHeaders);
    } else {
      ActionGraphBuilder graphBuilder = actionGraphBuilder;
      ImmutableSortedMap.Builder<Path, SourcePath> allHeadersBuilder =
          ImmutableSortedMap.naturalOrder();
      String platform = cxxPlatform.getFlavor().toString();
      ImmutableList<SourceSortedSet> platformHeaders =
          arg.getExportedPlatformHeaders().getMatchingValues(platform);

      return allHeadersBuilder
          .putAll(
              CxxDescriptionEnhancer.parseExportedHeaders(
                  targetNode.getBuildTarget(),
                  graphBuilder,
                  projectFilesystem,
                  Optional.empty(),
                  arg))
          .putAll(
              parseAllPlatformHeaders(
                  targetNode.getBuildTarget(),
                  graphBuilder.getSourcePathResolver(),
                  projectFilesystem,
                  platformHeaders,
                  true,
                  arg))
          .build();
    }
  }

  private ImmutableSortedMap<Path, SourcePath> getPrivateCxxHeaders(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode) {
    CxxLibraryDescription.CommonArg arg = targetNode.getConstructorArg();
    if (arg instanceof AppleNativeTargetDescriptionArg) {
      Path headerPathPrefix =
          AppleDescriptions.getHeaderPathPrefix(
              (AppleNativeTargetDescriptionArg) arg, targetNode.getBuildTarget());

      ImmutableSortedMap.Builder<String, SourcePath> fullHeadersBuilder =
          ImmutableSortedMap.naturalOrder();
      fullHeadersBuilder.putAll(
          AppleDescriptions.convertHeadersToPrivateCxxHeaders(
              targetNode.getBuildTarget(),
              projectSourcePathResolver::resolveSourcePath,
              headerPathPrefix,
              arg.getHeaders(),
              arg.getExportedHeaders()));

      for (Pair<Pattern, SourceSortedSet> patternMatchedHeader :
          arg.getExportedPlatformHeaders().getPatternsAndValues()) {
        fullHeadersBuilder.putAll(
            AppleDescriptions.convertHeadersToPrivateCxxHeaders(
                targetNode.getBuildTarget(),
                projectSourcePathResolver::resolveSourcePath,
                headerPathPrefix,
                SourceSortedSet.ofNamedSources(ImmutableSortedMap.of()),
                patternMatchedHeader.getSecond()));
      }

      for (Pair<Pattern, SourceSortedSet> patternMatchedHeader :
          arg.getPlatformHeaders().getPatternsAndValues()) {
        fullHeadersBuilder.putAll(
            AppleDescriptions.convertHeadersToPrivateCxxHeaders(
                targetNode.getBuildTarget(),
                projectSourcePathResolver::resolveSourcePath,
                headerPathPrefix,
                patternMatchedHeader.getSecond(),
                SourceSortedSet.ofNamedSources(ImmutableSortedMap.of())));
      }

      ImmutableSortedMap<String, SourcePath> fullHeaders = fullHeadersBuilder.build();
      return convertMapKeysToPaths(fullHeaders);
    } else {
      ActionGraphBuilder graphBuilder = actionGraphBuilder;
      ImmutableSortedMap.Builder<Path, SourcePath> allHeadersBuilder =
          ImmutableSortedMap.naturalOrder();
      String platform = cxxPlatform.getFlavor().toString();
      ImmutableList<SourceSortedSet> platformHeaders =
          arg.getPlatformHeaders().getMatchingValues(platform);

      return allHeadersBuilder
          .putAll(
              CxxDescriptionEnhancer.parseHeaders(
                  targetNode.getBuildTarget(),
                  graphBuilder,
                  projectFilesystem,
                  Optional.empty(),
                  arg))
          .putAll(
              parseAllPlatformHeaders(
                  targetNode.getBuildTarget(),
                  graphBuilder.getSourcePathResolver(),
                  projectFilesystem,
                  platformHeaders,
                  false,
                  arg))
          .build();
    }
  }

  private static ImmutableSortedMap<Path, SourcePath> convertMapKeysToPaths(
      ImmutableSortedMap<String, SourcePath> input) {
    ImmutableSortedMap.Builder<Path, SourcePath> output = ImmutableSortedMap.naturalOrder();
    for (Map.Entry<String, SourcePath> entry : input.entrySet()) {
      output.put(Paths.get(entry.getKey()), entry.getValue());
    }
    return output.build();
  }

  private void createPublicHeaderSymlinkTree(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode,
      Map<Path, SourcePath> contents)
      throws IOException {

    Path headerSymlinkTreeRoot = getPathToHeaderSymlinkTree(targetNode, HeaderVisibility.PUBLIC);

    LOG.verbose(
        "Building header symlink tree at %s with contents %s", headerSymlinkTreeRoot, contents);

    ImmutableSortedMap<Path, Path> resolvedContents =
        resolveHeaderContent(
            contents, ImmutableMap.of(), headerSymlinkTreeRoot, ImmutableList.builder());

    // This function has the unfortunate side effect of writing the symlink to disk (if needed).
    // This should prolly be cleaned up to be more explicit, but for now it makes sense to piggy
    // back off this existing functionality.
    shouldUpdateSymlinkTree(headerSymlinkTreeRoot, resolvedContents, true, ImmutableList.builder());
  }

  private void createPrivateHeaderSymlinkTree(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode,
      ImmutableSortedMap<Path, SourcePath> contents,
      ImmutableList.Builder<SourcePath> sourcePathsToBuildBuilder,
      boolean shouldCreateHeadersSymlinks,
      ImmutableList.Builder<Path> headerSymlinkTreesBuilder)
      throws IOException {

    contents.values().forEach(sourcePath -> sourcePathsToBuildBuilder.add(sourcePath));

    Path headerSymlinkTreeRoot = getPathToHeaderSymlinkTree(targetNode, HeaderVisibility.PRIVATE);

    LOG.verbose(
        "Building header symlink tree at %s with contents %s", headerSymlinkTreeRoot, contents);

    ImmutableSortedMap<Path, Path> resolvedContents =
        resolveHeaderContent(
            contents, ImmutableMap.of(), headerSymlinkTreeRoot, sourcePathsToBuildBuilder);

    if (!shouldUpdateSymlinkTree(
        headerSymlinkTreeRoot,
        resolvedContents,
        shouldCreateHeadersSymlinks,
        headerSymlinkTreesBuilder)) {
      return;
    }

    HeaderMap.Builder headerMapBuilder = new HeaderMap.Builder();
    for (Map.Entry<Path, SourcePath> entry : contents.entrySet()) {
      if (shouldCreateHeadersSymlinks) {
        headerMapBuilder.add(
            entry.getKey().toString(),
            getHeaderMapRelativeSymlinkPathForEntry(entry, headerSymlinkTreeRoot));
      } else {
        headerMapBuilder.add(
            entry.getKey().toString(),
            projectFilesystem
                .resolve(projectSourcePathResolver.resolveSourcePath(entry.getValue()))
                .getPath());
      }
    }

    Path headerMapLocation = getHeaderMapLocationFromSymlinkTreeRoot(headerSymlinkTreeRoot);
    projectFilesystem.writeBytesToPath(headerMapBuilder.build().getBytes(), headerMapLocation);
  }

  private ImmutableSortedMap<Path, Path> resolveHeaderContent(
      Map<Path, SourcePath> contents,
      ImmutableMap<Path, Path> nonSourcePaths,
      Path headerSymlinkTreeRoot,
      ImmutableList.Builder<SourcePath> sourcePathsToBuildBuilder) {
    ImmutableSortedMap.Builder<Path, Path> resolvedContentsBuilder =
        ImmutableSortedMap.naturalOrder();
    for (Map.Entry<Path, SourcePath> entry : contents.entrySet()) {
      Path link = headerSymlinkTreeRoot.resolve(entry.getKey());
      AbsPath existing =
          projectFilesystem.resolve(projectSourcePathResolver.resolveSourcePath(entry.getValue()));
      sourcePathsToBuildBuilder.add(entry.getValue());
      resolvedContentsBuilder.put(link, existing.getPath());
    }
    for (Map.Entry<Path, Path> entry : nonSourcePaths.entrySet()) {
      Path link = headerSymlinkTreeRoot.resolve(entry.getKey());
      resolvedContentsBuilder.put(link, entry.getValue());
    }
    ImmutableSortedMap<Path, Path> resolvedContents = resolvedContentsBuilder.build();
    return resolvedContents;
  }

  private boolean shouldUpdateSymlinkTree(
      Path headerSymlinkTreeRoot,
      ImmutableSortedMap<Path, Path> resolvedContents,
      boolean shouldCreateHeadersSymlinks,
      ImmutableList.Builder<Path> headerSymlinkTreesBuilder)
      throws IOException {
    Path hashCodeFilePath = headerSymlinkTreeRoot.resolve(".contents-hash");
    Optional<String> currentHashCode = projectFilesystem.readFileIfItExists(hashCodeFilePath);
    String newHashCode = getHeaderSymlinkTreeHashCode(resolvedContents, true, false).toString();

    headerSymlinkTreesBuilder.add(headerSymlinkTreeRoot);
    if (Optional.of(newHashCode).equals(currentHashCode)) {
      LOG.debug(
          "Symlink tree at %s is up to date, not regenerating (key %s).",
          headerSymlinkTreeRoot, newHashCode);
      return false;
    }
    LOG.debug(
        "Updating symlink tree at %s (old key %s, new key %s).",
        headerSymlinkTreeRoot, currentHashCode, newHashCode);
    projectFilesystem.deleteRecursivelyIfExists(headerSymlinkTreeRoot);
    projectFilesystem.mkdirs(headerSymlinkTreeRoot);
    if (shouldCreateHeadersSymlinks) {
      for (Map.Entry<Path, Path> entry : resolvedContents.entrySet()) {
        Path link = entry.getKey();
        Path existing = entry.getValue();
        projectFilesystem.createParentDirs(link);
        projectFilesystem.createSymLink(link, existing, /* force */ false);
      }
    }

    projectFilesystem.writeContentsToPath(newHashCode, hashCodeFilePath);

    return true;
  }

  private ImmutableSet<AbsPath> collectRecursivePublicSystemIncludeDirectories(
      TargetNode<?> targetNode) {
    return FluentIterable.from(
            AppleBuildRules.getRecursiveTargetNodeDependenciesOfTypes(
                xcodeDescriptions,
                targetGraph,
                Optional.of(dependenciesCache),
                AppleBuildRules.RecursiveDependenciesMode.BUILDING,
                targetNode,
                ImmutableSet.of(CxxLibraryDescription.class, AppleLibraryDescription.class)))
        .append(targetNode)
        .transformAndConcat(this::extractPublicSystemIncludeDirectories)
        .toSet();
  }

  private ImmutableSet<AbsPath> collectRecursivePublicIncludeDirectories(TargetNode<?> targetNode) {
    return FluentIterable.from(
            AppleBuildRules.getRecursiveTargetNodeDependenciesOfTypes(
                xcodeDescriptions,
                targetGraph,
                Optional.of(dependenciesCache),
                AppleBuildRules.RecursiveDependenciesMode.BUILDING,
                targetNode,
                ImmutableSet.of(CxxLibraryDescription.class, AppleLibraryDescription.class)))
        .append(targetNode)
        .transformAndConcat(this::extractPublicIncludeDirectories)
        .toSet();
  }

  private ImmutableSet<AbsPath> extractIncludeDirectories(TargetNode<?> targetNode) {
    AbsPath basePath =
        getFilesystemForTarget(Optional.of(targetNode.getBuildTarget()))
            .resolve(targetNode.getBuildTarget().getCellRelativeBasePath().getPath());
    ImmutableSortedSet<String> includeDirectories =
        TargetNodes.castArg(targetNode, CxxLibraryDescription.CommonArg.class)
            .map(input -> input.getConstructorArg().getIncludeDirectories())
            .orElse(ImmutableSortedSet.of());
    return FluentIterable.from(includeDirectories)
        .transform(includeDirectory -> basePath.resolve(includeDirectory).normalize())
        .toSet();
  }

  private ImmutableSet<Path> collectRecursiveHeaderSearchPaths(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode) {
    ImmutableSet.Builder<Path> builder = ImmutableSet.builder();

    builder.add(
        getHeaderSearchPathFromSymlinkTreeRoot(
            getHeaderSymlinkTreePath(targetNode, HeaderVisibility.PRIVATE)));
    Path absolutePath = projectFilesystem.resolve(getPathToMergedHeaderMap());
    builder.add(getHeaderSearchPathFromSymlinkTreeRoot(absolutePath));
    visitRecursivePrivateHeaderSymlinkTreesForTests(
        targetNode,
        (nativeNode, headerVisibility) -> {
          builder.add(
              getHeaderSearchPathFromSymlinkTreeRoot(
                  getHeaderSymlinkTreePath(nativeNode, headerVisibility)));
        });

    for (Path halideHeaderPath : collectRecursiveHalideLibraryHeaderPaths(targetNode)) {
      builder.add(halideHeaderPath);
    }

    return builder.build();
  }

  private ImmutableSet<Path> collectRecursiveSwiftIncludePaths(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode) {
    ImmutableSet.Builder<Path> builder = ImmutableSet.builder();
    final boolean enableIndexingFix = appleConfig.getEnableProjectV2SwiftIndexingFix();
    visitRecursiveHeaderSymlinkTrees(
        targetNode,
        (nativeNode, headerVisibility) -> {
          if ((!enableIndexingFix || nativeNode != targetNode)
              && headerVisibility.equals(HeaderVisibility.PUBLIC)) {
            Flavor defaultPlatformFlavor =
                targetNode.getConstructorArg().getDefaultPlatform().orElse(cxxPlatform.getFlavor());

            // Only "modular" libraries will have a modulemap generated for them.
            if (NodeHelper.isModularAppleLibrary(nativeNode)) {
              BuildTarget flavoredModuleMapTarget =
                  NodeHelper.getModularMapTarget(
                      nativeNode, HeaderMode.SYMLINK_TREE_WITH_MODULEMAP, defaultPlatformFlavor);

              RelPath symlinkTreePath =
                  CxxDescriptionEnhancer.getHeaderSymlinkTreePath(
                      projectFilesystem, flavoredModuleMapTarget, headerVisibility);
              builder.add(projectFilesystem.resolve(symlinkTreePath).getPath());
            }

            // However, Swift libraries that are not marked "modular" will still have a swiftmodule.
            if (enableIndexingFix
                && AppleDescriptions.targetNodeContainsSwiftSourceCode(nativeNode)) {
              BuildTarget flavoredSwiftCompileTarget =
                  NodeHelper.getSwiftModuleTarget(nativeNode, defaultPlatformFlavor);

              RelPath swiftModuleMap =
                  BuildTargetPaths.getGenPath(projectFilesystem, flavoredSwiftCompileTarget, "%s");
              builder.add(projectFilesystem.resolve(swiftModuleMap).getPath());
            }
          }
        });
    return builder.build();
  }

  /** Adds the set of headers defined by headerVisibility to the merged header maps. */
  private void addToMergedHeaderMap(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode,
      HeaderMap.Builder headerMapBuilder,
      ImmutableList.Builder<SourcePath> sourcePathsToBuildBuilder) {
    CxxLibraryDescription.CommonArg arg = targetNode.getConstructorArg();
    // If the target uses header symlinks, we need to use symlinks in the header map to support
    // accurate indexing/mapping of headers.
    boolean shouldCreateHeadersSymlinks =
        arg.getXcodePublicHeadersSymlinks().orElse(cxxBuckConfig.getPublicHeadersSymlinksEnabled());
    Path headerSymlinkTreeRoot = getPathToHeaderSymlinkTree(targetNode, HeaderVisibility.PUBLIC);

    AbsPath basePath;
    if (shouldCreateHeadersSymlinks) {
      basePath = projectFilesystem.getRootPath().resolve(headerSymlinkTreeRoot);
    } else {
      basePath = projectFilesystem.getRootPath();
    }
    ImmutableSortedMap<Path, SourcePath> publicCxxHeaders = getPublicCxxHeaders(targetNode);
    publicCxxHeaders.values().forEach(sourcePath -> sourcePathsToBuildBuilder.add(sourcePath));
    for (Map.Entry<Path, SourcePath> entry : publicCxxHeaders.entrySet()) {
      AbsPath path;
      if (shouldCreateHeadersSymlinks) {
        path = basePath.resolve(entry.getKey());
      } else {
        path = basePath.resolve(projectSourcePathResolver.resolveSourcePath(entry.getValue()));
      }
      headerMapBuilder.add(entry.getKey().toString(), path.getPath());
    }

    SwiftAttributes swiftAttributes = swiftAttributeParser.parseSwiftAttributes(targetNode);
    ImmutableMap<Path, Path> swiftHeaderMapEntries = swiftAttributes.publicHeaderMapEntries();
    for (Map.Entry<Path, Path> entry : swiftHeaderMapEntries.entrySet()) {
      headerMapBuilder.add(entry.getKey().toString(), entry.getValue());
    }
  }

  private Path getHeaderMapRelativeSymlinkPathForEntry(
      Map.Entry<Path, ?> entry, Path headerSymlinkTreeRoot) {
    return projectCell
        .getFilesystem()
        .resolve(projectCell.getFilesystem().getBuckPaths().getConfiguredBuckOut())
        .normalize()
        .relativize(
            projectCell
                .getFilesystem()
                .resolve(headerSymlinkTreeRoot)
                .resolve(entry.getKey())
                .normalize())
        .getPath();
  }

  private HashCode getHeaderSymlinkTreeHashCode(
      ImmutableSortedMap<Path, Path> contents,
      boolean shouldCreateHeadersSymlinks,
      boolean shouldCreateHeaderMap) {
    Hasher hasher = Hashing.sha1().newHasher();
    hasher.putBytes(ruleKeyConfiguration.getCoreKey().getBytes(StandardCharsets.UTF_8));
    String symlinkState = shouldCreateHeadersSymlinks ? "symlinks-enabled" : "symlinks-disabled";
    byte[] symlinkStateValue = symlinkState.getBytes(StandardCharsets.UTF_8);
    hasher.putInt(symlinkStateValue.length);
    hasher.putBytes(symlinkStateValue);
    String hmapState = shouldCreateHeaderMap ? "hmap-enabled" : "hmap-disabled";
    byte[] hmapStateValue = hmapState.getBytes(StandardCharsets.UTF_8);
    hasher.putInt(hmapStateValue.length);
    hasher.putBytes(hmapStateValue);
    hasher.putInt(0);
    for (Map.Entry<Path, Path> entry : contents.entrySet()) {
      byte[] key = entry.getKey().toString().getBytes(StandardCharsets.UTF_8);
      byte[] value = entry.getValue().toString().getBytes(StandardCharsets.UTF_8);
      hasher.putInt(key.length);
      hasher.putBytes(key);
      hasher.putInt(value.length);
      hasher.putBytes(value);
    }
    return hasher.hash();
  }

  private ImmutableSet<Path> collectRecursiveHalideLibraryHeaderPaths(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode) {
    ImmutableSet.Builder<Path> builder = ImmutableSet.builder();
    for (TargetNode<?> input :
        AppleBuildRules.getRecursiveTargetNodeDependenciesOfTypes(
            xcodeDescriptions,
            targetGraph,
            Optional.of(dependenciesCache),
            AppleBuildRules.RecursiveDependenciesMode.BUILDING,
            targetNode,
            Optional.of(ImmutableSet.of(HalideLibraryDescription.class)))) {
      TargetNode<HalideLibraryDescriptionArg> halideNode =
          TargetNodes.castArg(input, HalideLibraryDescriptionArg.class).get();
      BuildTarget buildTarget = halideNode.getBuildTarget();
      builder.add(
          pathRelativizer.outputDirToRootRelative(
              HalideCompile.headerOutputPath(
                      buildTarget.withFlavors(
                          HalideLibraryDescription.HALIDE_COMPILE_FLAVOR, cxxPlatform.getFlavor()),
                      projectFilesystem,
                      halideNode.getConstructorArg().getFunctionName())
                  .getParent()));
    }
    return builder.build();
  }

  private void visitRecursiveHeaderSymlinkTrees(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode,
      BiConsumer<TargetNode<? extends CxxLibraryDescription.CommonArg>, HeaderVisibility> visitor) {
    // Visits public and private headers from current target.
    visitor.accept(targetNode, HeaderVisibility.PRIVATE);
    visitor.accept(targetNode, HeaderVisibility.PUBLIC);

    // Visits public headers from dependencies.
    for (TargetNode<?> input :
        AppleBuildRules.getRecursiveTargetNodeDependenciesOfTypes(
            xcodeDescriptions,
            targetGraph,
            Optional.of(dependenciesCache),
            AppleBuildRules.RecursiveDependenciesMode.BUILDING,
            targetNode,
            Optional.of(xcodeDescriptions.getXCodeDescriptions()))) {
      NodeHelper.getAppleNativeNode(targetGraph, input)
          .ifPresent(argTargetNode -> visitor.accept(argTargetNode, HeaderVisibility.PUBLIC));
    }

    visitRecursivePrivateHeaderSymlinkTreesForTests(targetNode, visitor);
  }

  /**
   * @return Whether the {@code testNode} is listed as a test of {@code nativeNode} or {@code
   *     dependencyNode}.
   */
  private boolean isSourceUnderTest(
      TargetNode<CxxLibraryDescription.CommonArg> nativeNode,
      TargetNode<?> dependencyNode,
      TargetNode<?> testNode) {
    boolean isSourceUnderTest =
        nativeNode.getConstructorArg().getTests().contains(testNode.getBuildTarget());

    if (dependencyNode != nativeNode && dependencyNode.getConstructorArg() instanceof HasTests) {
      ImmutableSortedSet<BuildTarget> tests =
          ((HasTests) dependencyNode.getConstructorArg()).getTests();
      if (tests.contains(testNode.getBuildTarget())) {
        isSourceUnderTest = true;
      }
    }

    return isSourceUnderTest;
  }

  private ImmutableSet<AbsPath> extractPublicIncludeDirectories(TargetNode<?> targetNode) {
    AbsPath basePath =
        getFilesystemForTarget(Optional.of(targetNode.getBuildTarget()))
            .resolve(targetNode.getBuildTarget().getCellRelativeBasePath().getPath());
    ImmutableSortedSet<String> includeDirectories =
        TargetNodes.castArg(targetNode, CxxLibraryDescription.CommonArg.class)
            .map(input -> input.getConstructorArg().getPublicIncludeDirectories())
            .orElse(ImmutableSortedSet.of());
    return FluentIterable.from(includeDirectories)
        .transform(includeDirectory -> basePath.resolve(includeDirectory).normalize())
        .toSet();
  }

  private ImmutableSet<AbsPath> extractPublicSystemIncludeDirectories(TargetNode<?> targetNode) {
    AbsPath basePath =
        getFilesystemForTarget(Optional.of(targetNode.getBuildTarget()))
            .resolve(targetNode.getBuildTarget().getCellRelativeBasePath().getPath());
    ImmutableSortedSet<String> includeDirectories =
        TargetNodes.castArg(targetNode, CxxLibraryDescription.CommonArg.class)
            .map(input -> input.getConstructorArg().getPublicSystemIncludeDirectories())
            .orElse(ImmutableSortedSet.of());
    return FluentIterable.from(includeDirectories)
        .transform(includeDirectory -> basePath.resolve(includeDirectory).normalize())
        .toSet();
  }

  private RelPath getPathToGenDirRelativeToProjectFileSystem(ProjectFilesystem targetFileSystem) {
    // For targets in the cell of the project, this will simply return the normal `buck-out/gen`
    // path. However, for targets in other cells, we need to put them in `buck-out/cell/...` path
    // In order to do this, we need to get the target file system and relativize the path back
    // to the project cell, else this will not go in the right place.
    //
    // So for a project in foo//bar/baz:
    //    foo//bar/baz:target -> ./buck-out/gen/...
    //    foo//qux:target -> ./buck-out/cells/qux/...
    return projectFilesystem.relativize(
        targetFileSystem.resolve(targetFileSystem.getBuckPaths().getGenDir()));
  }

  private Path getPathToHeaderMapsRoot(ProjectFilesystem targetFileSystem) {
    RelPath genDirPathForTarget = getPathToGenDirRelativeToProjectFileSystem(targetFileSystem);
    return genDirPathForTarget.resolve("_p");
  }

  private static Path getHeaderMapLocationFromSymlinkTreeRoot(Path headerSymlinkTreeRoot) {
    return headerSymlinkTreeRoot.resolve(".hmap");
  }

  private static Path getHeaderSearchPathFromSymlinkTreeRoot(Path headerSymlinkTreeRoot) {
    return getHeaderMapLocationFromSymlinkTreeRoot(headerSymlinkTreeRoot);
  }

  private ProjectFilesystem getFilesystemForTarget(Optional<BuildTarget> target) {
    if (target.isPresent()) {
      Cell cell = projectCell.getCellProvider().getCellByCanonicalCellName(target.get().getCell());
      return cell.getFilesystem();
    } else {
      return projectFilesystem;
    }
  }

  private static Path getFilenameToHeadersPath(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode, String suffix) {
    String hashedPath =
        BaseEncoding.base64Url()
            .omitPadding()
            .encode(
                Hashing.sha1()
                    .hashString(
                        targetNode
                            .getBuildTarget()
                            .getUnflavoredBuildTarget()
                            .getFullyQualifiedName(),
                        StandardCharsets.UTF_8)
                    .asBytes())
            .substring(0, 10);
    return Paths.get(hashedPath + suffix);
  }

  private Path getPathToHeadersPath(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode, String suffix) {
    return getPathToHeaderMapsRoot(getFilesystemForTarget(Optional.of(targetNode.getBuildTarget())))
        .resolve(getFilenameToHeadersPath(targetNode, suffix));
  }

  private Path getAbsolutePathToHeaderSymlinkTree(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode,
      HeaderVisibility headerVisibility) {
    return projectFilesystem.resolve(getPathToHeaderSymlinkTree(targetNode, headerVisibility));
  }

  public Path getPathToHeaderSymlinkTree(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode,
      HeaderVisibility headerVisibility) {
    return getPathToHeadersPath(
        targetNode, AppleHeaderVisibilities.getHeaderSymlinkTreeSuffix(headerVisibility));
  }

  /** @param targetNode Must have a header symlink tree or an exception will be thrown. */
  private Path getHeaderSymlinkTreePath(
      TargetNode<? extends CxxLibraryDescription.CommonArg> targetNode,
      HeaderVisibility headerVisibility) {
    Path treeRoot = getAbsolutePathToHeaderSymlinkTree(targetNode, headerVisibility);
    return treeRoot;
  }

  private Path getPathToMergedHeaderMap() {
    return getPathToHeaderMapsRoot(projectFilesystem).resolve("pub-hmap");
  }

  /** @return a map of all exported platform headers without matching a specific platform. */
  private static ImmutableMap<Path, SourcePath> parseAllPlatformHeaders(
      BuildTarget buildTarget,
      SourcePathResolverAdapter sourcePathResolverAdapter,
      ProjectFilesystem filesystem,
      ImmutableList<SourceSortedSet> platformHeaders,
      boolean export,
      CxxLibraryDescription.CommonArg args) {
    ImmutableMap.Builder<String, SourcePath> parsed = ImmutableMap.builder();

    String parameterName = (export) ? "exported_platform_headers" : "platform_headers";

    // Include all platform specific headers.
    for (SourceSortedSet sourceList : platformHeaders) {
      parsed.putAll(
          sourceList.toNameMap(
              buildTarget, sourcePathResolverAdapter, parameterName, path -> true, path -> path));
    }
    return CxxPreprocessables.resolveHeaderMap(
        args.getHeaderNamespace()
            .map(Paths::get)
            .orElse(
                buildTarget.getCellRelativeBasePath().getPath().toPath(filesystem.getFileSystem())),
        parsed.build());
  }
}
