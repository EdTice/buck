// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: downward_api.proto

package com.facebook.buck.downward.model;

@javax.annotation.Generated(value="protoc", comments="annotations:EventTypeMessageOrBuilder.java.pb.meta")
public interface EventTypeMessageOrBuilder extends
    // @@protoc_insertion_point(interface_extends:downward.api.v1.EventTypeMessage)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.downward.api.v1.EventTypeMessage.EventType event_type = 1;</code>
   */
  int getEventTypeValue();
  /**
   * <code>.downward.api.v1.EventTypeMessage.EventType event_type = 1;</code>
   */
  com.facebook.buck.downward.model.EventTypeMessage.EventType getEventType();

  /**
   * <pre>
   * would be used if `event_type` is set to `CUSTOM_EVENT`
   * </pre>
   *
   * <code>int32 custom_event_type_code = 2;</code>
   */
  int getCustomEventTypeCode();
}