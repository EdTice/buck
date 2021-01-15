// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: javacd.proto

package com.facebook.buck.javacd.model;

@javax.annotation.Generated(value="protoc", comments="annotations:JarParametersOrBuilder.java.pb.meta")
public interface JarParametersOrBuilder extends
    // @@protoc_insertion_point(interface_extends:javacd.api.v1.JarParameters)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>bool hashEntries = 1;</code>
   */
  boolean getHashEntries();

  /**
   * <code>bool mergeManifests = 2;</code>
   */
  boolean getMergeManifests();

  /**
   * <code>bool disallowAllDuplicates = 3;</code>
   */
  boolean getDisallowAllDuplicates();

  /**
   * <code>.javacd.api.v1.RelPath jarPath = 4;</code>
   */
  boolean hasJarPath();
  /**
   * <code>.javacd.api.v1.RelPath jarPath = 4;</code>
   */
  com.facebook.buck.javacd.model.RelPath getJarPath();
  /**
   * <code>.javacd.api.v1.RelPath jarPath = 4;</code>
   */
  com.facebook.buck.javacd.model.RelPathOrBuilder getJarPathOrBuilder();

  /**
   * <code>.javacd.api.v1.JarParameters.RemoveClassesPatternsMatcher removeEntryPredicate = 5;</code>
   */
  boolean hasRemoveEntryPredicate();
  /**
   * <code>.javacd.api.v1.JarParameters.RemoveClassesPatternsMatcher removeEntryPredicate = 5;</code>
   */
  com.facebook.buck.javacd.model.JarParameters.RemoveClassesPatternsMatcher getRemoveEntryPredicate();
  /**
   * <code>.javacd.api.v1.JarParameters.RemoveClassesPatternsMatcher removeEntryPredicate = 5;</code>
   */
  com.facebook.buck.javacd.model.JarParameters.RemoveClassesPatternsMatcherOrBuilder getRemoveEntryPredicateOrBuilder();

  /**
   * <code>repeated .javacd.api.v1.RelPath entriesToJar = 6;</code>
   */
  java.util.List<com.facebook.buck.javacd.model.RelPath> 
      getEntriesToJarList();
  /**
   * <code>repeated .javacd.api.v1.RelPath entriesToJar = 6;</code>
   */
  com.facebook.buck.javacd.model.RelPath getEntriesToJar(int index);
  /**
   * <code>repeated .javacd.api.v1.RelPath entriesToJar = 6;</code>
   */
  int getEntriesToJarCount();
  /**
   * <code>repeated .javacd.api.v1.RelPath entriesToJar = 6;</code>
   */
  java.util.List<? extends com.facebook.buck.javacd.model.RelPathOrBuilder> 
      getEntriesToJarOrBuilderList();
  /**
   * <code>repeated .javacd.api.v1.RelPath entriesToJar = 6;</code>
   */
  com.facebook.buck.javacd.model.RelPathOrBuilder getEntriesToJarOrBuilder(
      int index);

  /**
   * <code>repeated .javacd.api.v1.RelPath overrideEntriesToJar = 7;</code>
   */
  java.util.List<com.facebook.buck.javacd.model.RelPath> 
      getOverrideEntriesToJarList();
  /**
   * <code>repeated .javacd.api.v1.RelPath overrideEntriesToJar = 7;</code>
   */
  com.facebook.buck.javacd.model.RelPath getOverrideEntriesToJar(int index);
  /**
   * <code>repeated .javacd.api.v1.RelPath overrideEntriesToJar = 7;</code>
   */
  int getOverrideEntriesToJarCount();
  /**
   * <code>repeated .javacd.api.v1.RelPath overrideEntriesToJar = 7;</code>
   */
  java.util.List<? extends com.facebook.buck.javacd.model.RelPathOrBuilder> 
      getOverrideEntriesToJarOrBuilderList();
  /**
   * <code>repeated .javacd.api.v1.RelPath overrideEntriesToJar = 7;</code>
   */
  com.facebook.buck.javacd.model.RelPathOrBuilder getOverrideEntriesToJarOrBuilder(
      int index);

  /**
   * <code>string mainClass = 8;</code>
   */
  java.lang.String getMainClass();
  /**
   * <code>string mainClass = 8;</code>
   */
  com.google.protobuf.ByteString
      getMainClassBytes();

  /**
   * <code>.javacd.api.v1.RelPath manifestFile = 9;</code>
   */
  boolean hasManifestFile();
  /**
   * <code>.javacd.api.v1.RelPath manifestFile = 9;</code>
   */
  com.facebook.buck.javacd.model.RelPath getManifestFile();
  /**
   * <code>.javacd.api.v1.RelPath manifestFile = 9;</code>
   */
  com.facebook.buck.javacd.model.RelPathOrBuilder getManifestFileOrBuilder();

  /**
   * <code>.javacd.api.v1.JarParameters.LogLevel duplicatesLogLevel = 10;</code>
   */
  int getDuplicatesLogLevelValue();
  /**
   * <code>.javacd.api.v1.JarParameters.LogLevel duplicatesLogLevel = 10;</code>
   */
  com.facebook.buck.javacd.model.JarParameters.LogLevel getDuplicatesLogLevel();
}