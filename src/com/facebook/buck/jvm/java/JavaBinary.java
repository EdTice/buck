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

package com.facebook.buck.jvm.java;

import com.facebook.buck.core.build.buildable.context.BuildableContext;
import com.facebook.buck.core.build.context.BuildContext;
import com.facebook.buck.core.filesystems.RelPath;
import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.OutputLabel;
import com.facebook.buck.core.model.impl.BuildTargetPaths;
import com.facebook.buck.core.rulekey.AddToRuleKey;
import com.facebook.buck.core.rules.BuildRule;
import com.facebook.buck.core.rules.BuildRuleParams;
import com.facebook.buck.core.rules.BuildRuleResolver;
import com.facebook.buck.core.rules.attr.HasRuntimeDeps;
import com.facebook.buck.core.rules.impl.AbstractBuildRuleWithDeclaredAndExtraDeps;
import com.facebook.buck.core.rules.tool.BinaryBuildRule;
import com.facebook.buck.core.sourcepath.ExplicitBuildTargetSourcePath;
import com.facebook.buck.core.sourcepath.PathSourcePath;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolverAdapter;
import com.facebook.buck.core.toolchain.tool.Tool;
import com.facebook.buck.core.toolchain.tool.impl.CommandTool;
import com.facebook.buck.io.BuildCellRelativePath;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.jvm.core.HasClasspathDeps;
import com.facebook.buck.jvm.core.HasClasspathEntries;
import com.facebook.buck.jvm.core.JavaLibrary;
import com.facebook.buck.rules.args.SourcePathArg;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.fs.MakeCleanDirectoryStep;
import com.facebook.buck.step.fs.MkdirStep;
import com.facebook.buck.step.fs.SymlinkFileStep;
import com.facebook.buck.step.isolatedsteps.java.JarDirectoryStep;
import com.facebook.buck.util.PatternsMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import javax.annotation.Nullable;

@BuildsAnnotationProcessor
public class JavaBinary extends AbstractBuildRuleWithDeclaredAndExtraDeps
    implements BinaryBuildRule, HasClasspathDeps, HasClasspathEntries, HasRuntimeDeps {

  // We're just propagating the runtime launcher through `getExecutable`, so don't add it to the
  // rule key.
  private final Tool javaRuntimeLauncher;

  @AddToRuleKey @Nullable private final String mainClass;

  @AddToRuleKey @Nullable private final SourcePath manifestFile;
  private final boolean mergeManifests;
  private final boolean disallowAllDuplicates;

  @Nullable @AddToRuleKey private final SourcePath metaInfDirectory;

  @SuppressWarnings("PMD.UnusedPrivateField")
  @AddToRuleKey
  private final ImmutableSet<Pattern> blacklist;

  private final PatternsMatcher blacklistPatternsMatcher;

  private final ImmutableSet<JavaLibrary> transitiveClasspathDeps;
  private final ImmutableSet<SourcePath> transitiveClasspaths;

  private final boolean cache;
  private final Level duplicatesLogLevel;

  public JavaBinary(
      BuildTarget buildTarget,
      ProjectFilesystem projectFilesystem,
      BuildRuleParams params,
      Tool javaRuntimeLauncher,
      @Nullable String mainClass,
      @Nullable SourcePath manifestFile,
      boolean mergeManifests,
      boolean disallowAllDuplicates,
      @Nullable Path metaInfDirectory,
      ImmutableSet<Pattern> blacklist,
      ImmutableSet<JavaLibrary> transitiveClasspathDeps,
      ImmutableSet<SourcePath> transitiveClasspaths,
      boolean cache,
      Level duplicatesLogLevel) {
    super(buildTarget, projectFilesystem, params);
    this.javaRuntimeLauncher = javaRuntimeLauncher;
    this.mainClass = mainClass;
    this.manifestFile = manifestFile;
    this.mergeManifests = mergeManifests;
    this.disallowAllDuplicates = disallowAllDuplicates;
    this.metaInfDirectory =
        metaInfDirectory != null
            ? PathSourcePath.of(getProjectFilesystem(), metaInfDirectory)
            : null;
    this.blacklist = blacklist;
    blacklistPatternsMatcher = new PatternsMatcher(blacklist);
    this.transitiveClasspathDeps = transitiveClasspathDeps;
    this.transitiveClasspaths = transitiveClasspaths;
    this.cache = cache;
    this.duplicatesLogLevel = duplicatesLogLevel;
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext context, BuildableContext buildableContext) {

    ImmutableList.Builder<Step> commands = ImmutableList.builder();

    RelPath outputDirectory = getOutputDirectory();
    ProjectFilesystem filesystem = getProjectFilesystem();
    Step mkdir =
        MkdirStep.of(
            BuildCellRelativePath.fromCellRelativePath(
                context.getBuildCellRootPath(), filesystem, outputDirectory));
    commands.add(mkdir);

    ImmutableSortedSet<RelPath> includePaths;
    ImmutableSortedSet<RelPath> overrideIncludePaths = ImmutableSortedSet.of();
    SourcePathResolverAdapter sourcePathResolver = context.getSourcePathResolver();
    if (metaInfDirectory != null) {
      RelPath stagingRoot = outputDirectory.resolveRel("meta_inf_staging");
      RelPath stagingTarget = stagingRoot.resolveRel("META-INF");

      commands.addAll(
          MakeCleanDirectoryStep.of(
              BuildCellRelativePath.fromCellRelativePath(
                  context.getBuildCellRootPath(), filesystem, stagingRoot)));

      commands.add(
          SymlinkFileStep.of(
              filesystem,
              sourcePathResolver.getCellUnsafeRelPath(filesystem, metaInfDirectory).getPath(),
              stagingTarget.getPath()));

      overrideIncludePaths =
          ImmutableSortedSet.orderedBy(RelPath.comparator()).add(stagingRoot).build();
      includePaths =
          ImmutableSortedSet.orderedBy(RelPath.comparator())
              .addAll(sourcePathResolver.getAllRelativePaths(filesystem, getTransitiveClasspaths()))
              .build();
    } else {
      includePaths = sourcePathResolver.getAllRelativePaths(filesystem, getTransitiveClasspaths());
    }

    RelPath outputFile =
        sourcePathResolver.getCellUnsafeRelPath(filesystem, getSourcePathToOutput());
    RelPath manifestPath =
        manifestFile == null
            ? null
            : sourcePathResolver.getCellUnsafeRelPath(filesystem, manifestFile);

    Step jar =
        new JarDirectoryStep(
            JarParameters.builder()
                .setJarPath(outputFile)
                .setEntriesToJar(includePaths)
                .setOverrideEntriesToJar(overrideIncludePaths)
                .setMainClass(Optional.ofNullable(mainClass))
                .setManifestFile(Optional.ofNullable(manifestPath))
                .setMergeManifests(mergeManifests)
                .setDisallowAllDuplicates(disallowAllDuplicates)
                .setDuplicatesLogLevel(duplicatesLogLevel)
                .setRemoveEntryPredicate(
                    entry ->
                        blacklistPatternsMatcher.substringMatches(((ZipEntry) entry).getName()))
                .build());
    commands.add(jar);

    buildableContext.recordArtifact(outputFile.getPath());
    return commands.build();
  }

  @Override
  public ImmutableSet<SourcePath> getTransitiveClasspaths() {
    return transitiveClasspaths;
  }

  @Override
  public ImmutableSet<JavaLibrary> getTransitiveClasspathDeps() {
    return transitiveClasspathDeps;
  }

  @Override
  public ImmutableSet<SourcePath> getImmediateClasspaths() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<SourcePath> getOutputClasspaths() {
    // A binary has no exported deps or classpath contributions of its own
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<SourcePath> getCompileTimeClasspathSourcePaths() {
    return ImmutableSet.of();
  }

  private RelPath getOutputDirectory() {
    return BuildTargetPaths.getGenPath(
            getProjectFilesystem().getBuckPaths(), getBuildTarget(), "%s")
        .getParent();
  }

  @Override
  public SourcePath getSourcePathToOutput() {
    return ExplicitBuildTargetSourcePath.of(
        getBuildTarget(),
        Paths.get(
            String.format(
                "%s/%s.jar",
                getOutputDirectory(), getBuildTarget().getShortNameAndFlavorPostfix())));
  }

  @Override
  public Tool getExecutableCommand(OutputLabel outputLabel) {
    Preconditions.checkState(
        mainClass != null,
        "Must specify a main class for %s in order to to run it.",
        getBuildTarget());

    return new CommandTool.Builder(javaRuntimeLauncher)
        .addArg("-jar")
        .addArg(SourcePathArg.of(getSourcePathToOutput()))
        .build();
  }

  @Override
  public boolean isCacheable() {
    return cache;
  }

  @Override
  public Set<BuildRule> getDepsForTransitiveClasspathEntries() {
    return ImmutableSortedSet.copyOf(getTransitiveClasspathDeps());
  }

  @Override
  public Stream<BuildTarget> getRuntimeDeps(BuildRuleResolver buildRuleResolver) {
    return getTransitiveClasspathDeps().stream()
        .flatMap(lib -> lib.getRuntimeDeps(buildRuleResolver));
  }
}
