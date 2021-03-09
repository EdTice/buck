// @generated
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: src/com/facebook/buck/remoteexecution/proto/metadata.proto

package com.facebook.buck.remoteexecution.proto;

@javax.annotation.Generated(value="protoc", comments="annotations:RemoteExecutionMetadataProto.java.pb.meta")
public final class RemoteExecutionMetadataProto {
  private RemoteExecutionMetadataProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_TraceInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_TraceInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_RESessionID_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_RESessionID_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_BuckInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_BuckInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_CreatorInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_CreatorInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_ExecutionEngineInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_ExecutionEngineInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_WorkerInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_WorkerInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_ManifoldBucket_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_ManifoldBucket_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_CasClientInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_CasClientInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_CapabilityValue_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_CapabilityValue_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_WorkerRequirements_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_WorkerRequirements_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_ClientJobInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_ClientJobInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_ClientActionInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_ClientActionInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_ExecutedActionInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_ExecutedActionInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_DebugInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_DebugInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_facebook_remote_execution_RemoteExecutionMetadata_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_facebook_remote_execution_RemoteExecutionMetadata_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n:src/com/facebook/buck/remoteexecution/" +
      "proto/metadata.proto\022\031facebook.remote_ex" +
      "ecution\032\036google/protobuf/wrappers.proto\"" +
      ".\n\tTraceInfo\022\020\n\010trace_id\030\001 \001(\t\022\017\n\007edge_i" +
      "d\030\002 \001(\t\"\031\n\013RESessionID\022\n\n\002id\030\001 \001(\t\"\210\001\n\010B" +
      "uckInfo\022\020\n\010build_id\030\001 \001(\t\022\021\n\trule_name\030\002" +
      " \001(\t\022\033\n\023auxiliary_build_tag\030\003 \001(\t\022\026\n\016pro" +
      "ject_prefix\030\004 \001(\t\022\017\n\007version\030\005 \001(\t\022\021\n\tru" +
      "le_type\030\006 \001(\t\"4\n\013CreatorInfo\022\020\n\010username" +
      "\030\001 \001(\t\022\023\n\013client_type\030\002 \001(\t\"\'\n\023Execution" +
      "EngineInfo\022\020\n\010hostname\030\001 \001(\t\"6\n\nWorkerIn" +
      "fo\022\020\n\010hostname\030\001 \001(\t\022\026\n\016execution_path\030\002" +
      " \001(\t\"\036\n\016ManifoldBucket\022\014\n\004name\030\001 \001(\t\"\357\001\n" +
      "\rCasClientInfo\022\014\n\004name\030\001 \001(\t\022\016\n\006job_id\030\002" +
      " \001(\t\022Q\n\016streaming_mode\030\003 \001(\01629.facebook." +
      "remote_execution.CasClientInfo.CasStream" +
      "ingMode\022B\n\017manifold_bucket\030\004 \001(\0132).faceb" +
      "ook.remote_execution.ManifoldBucket\")\n\020C" +
      "asStreamingMode\022\013\n\007BUCKETS\020\000\022\010\n\004HTTP\020\001\" " +
      "\n\017CapabilityValue\022\r\n\005value\030\001 \001(\t\"\204\004\n\022Wor" +
      "kerRequirements\022M\n\013worker_size\030\001 \001(\01628.f" +
      "acebook.remote_execution.WorkerRequireme" +
      "nts.WorkerSize\022W\n\rplatform_type\030\002 \001(\0162@." +
      "facebook.remote_execution.WorkerRequirem" +
      "ents.WorkerPlatformType\022\'\n\037should_try_la" +
      "rger_worker_on_oom\030\003 \001(\010\022;\n\007testing\030\004 \001(" +
      "\0132*.facebook.remote_execution.Capability" +
      "Value\022=\n\ttask_name\030\005 \001(\0132*.facebook.remo" +
      "te_execution.CapabilityValue\"X\n\022WorkerPl" +
      "atformType\022\t\n\005LINUX\020\000\022\024\n\020ANDROID_EMULATO" +
      "R\020\001\022\r\n\tLINUX_GPU\020\002\022\022\n\016BROWSER_CHROME\020\003\"G" +
      "\n\nWorkerSize\022\t\n\005SMALL\020\000\022\n\n\006MEDIUM\020\001\022\t\n\005L" +
      "ARGE\020\002\022\n\n\006XLARGE\020\003\022\013\n\007XXLARGE\020\004\"l\n\rClien" +
      "tJobInfo\022\030\n\020deployment_stage\030\001 \001(\t\022\023\n\013in" +
      "stance_id\030\002 \001(\t\022\020\n\010group_id\030\003 \001(\t\022\032\n\022cli" +
      "ent_side_tenant\030\004 \001(\t\"j\n\020ClientActionInf" +
      "o\022\022\n\nrepository\030\001 \001(\t\022\025\n\rschedule_type\030\002" +
      " \001(\t\022\030\n\020re_session_label\030\003 \001(\t\022\021\n\ttenant" +
      "_id\030\004 \001(\t\"\333\002\n\022ExecutedActionInfo\022\033\n\023cpu_" +
      "stat_usage_usec\030\001 \001(\003\022\032\n\022cpu_stat_user_u" +
      "sec\030\002 \001(\003\022\034\n\024cpu_stat_system_usec\030\003 \001(\003\022" +
      "L\n(is_fallback_enabled_for_completed_act" +
      "ion\030\004 \001(\0132\032.google.protobuf.BoolValue\022\036\n" +
      "\026engine_scheduled_count\030\005 \001(\003\022\024\n\014max_use" +
      "d_mem\030\006 \001(\003\022\026\n\016host_total_mem\030\007 \001(\003\022\026\n\016t" +
      "ask_total_mem\030\010 \001(\003\022$\n\034executed_on_elast" +
      "ic_capacity\030\t \001(\010\022\024\n\014deduplicated\030\n \001(\010\"" +
      "/\n\tDebugInfo\022\"\n\032pause_before_clean_timeo" +
      "ut\030\001 \001(\r\"\253\006\n\027RemoteExecutionMetadata\022=\n\r" +
      "re_session_id\030\001 \001(\0132&.facebook.remote_ex" +
      "ecution.RESessionID\0226\n\tbuck_info\030\002 \001(\0132#" +
      ".facebook.remote_execution.BuckInfo\0228\n\nt" +
      "race_info\030\003 \001(\0132$.facebook.remote_execut" +
      "ion.TraceInfo\022<\n\014creator_info\030\004 \001(\0132&.fa" +
      "cebook.remote_execution.CreatorInfo\022C\n\013e" +
      "ngine_info\030\005 \001(\0132..facebook.remote_execu" +
      "tion.ExecutionEngineInfo\022:\n\013worker_info\030" +
      "\006 \001(\0132%.facebook.remote_execution.Worker" +
      "Info\022A\n\017cas_client_info\030\007 \001(\0132(.facebook" +
      ".remote_execution.CasClientInfo\022J\n\023worke" +
      "r_requirements\030\010 \001(\0132-.facebook.remote_e" +
      "xecution.WorkerRequirements\022G\n\022client_ac" +
      "tion_info\030\n \001(\0132+.facebook.remote_execut" +
      "ion.ClientActionInfo\022K\n\024executed_action_" +
      "info\030\013 \001(\0132-.facebook.remote_execution.E" +
      "xecutedActionInfo\0228\n\ndebug_info\030\014 \001(\0132$." +
      "facebook.remote_execution.DebugInfo\022A\n\017c" +
      "lient_job_info\030\r \001(\0132(.facebook.remote_e" +
      "xecution.ClientJobInfoBI\n\'com.facebook.b" +
      "uck.remoteexecution.protoB\034RemoteExecuti" +
      "onMetadataProtoP\001b\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.WrappersProto.getDescriptor(),
        }, assigner);
    internal_static_facebook_remote_execution_TraceInfo_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_facebook_remote_execution_TraceInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_TraceInfo_descriptor,
        new java.lang.String[] { "TraceId", "EdgeId", });
    internal_static_facebook_remote_execution_RESessionID_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_facebook_remote_execution_RESessionID_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_RESessionID_descriptor,
        new java.lang.String[] { "Id", });
    internal_static_facebook_remote_execution_BuckInfo_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_facebook_remote_execution_BuckInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_BuckInfo_descriptor,
        new java.lang.String[] { "BuildId", "RuleName", "AuxiliaryBuildTag", "ProjectPrefix", "Version", "RuleType", });
    internal_static_facebook_remote_execution_CreatorInfo_descriptor =
      getDescriptor().getMessageTypes().get(3);
    internal_static_facebook_remote_execution_CreatorInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_CreatorInfo_descriptor,
        new java.lang.String[] { "Username", "ClientType", });
    internal_static_facebook_remote_execution_ExecutionEngineInfo_descriptor =
      getDescriptor().getMessageTypes().get(4);
    internal_static_facebook_remote_execution_ExecutionEngineInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_ExecutionEngineInfo_descriptor,
        new java.lang.String[] { "Hostname", });
    internal_static_facebook_remote_execution_WorkerInfo_descriptor =
      getDescriptor().getMessageTypes().get(5);
    internal_static_facebook_remote_execution_WorkerInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_WorkerInfo_descriptor,
        new java.lang.String[] { "Hostname", "ExecutionPath", });
    internal_static_facebook_remote_execution_ManifoldBucket_descriptor =
      getDescriptor().getMessageTypes().get(6);
    internal_static_facebook_remote_execution_ManifoldBucket_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_ManifoldBucket_descriptor,
        new java.lang.String[] { "Name", });
    internal_static_facebook_remote_execution_CasClientInfo_descriptor =
      getDescriptor().getMessageTypes().get(7);
    internal_static_facebook_remote_execution_CasClientInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_CasClientInfo_descriptor,
        new java.lang.String[] { "Name", "JobId", "StreamingMode", "ManifoldBucket", });
    internal_static_facebook_remote_execution_CapabilityValue_descriptor =
      getDescriptor().getMessageTypes().get(8);
    internal_static_facebook_remote_execution_CapabilityValue_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_CapabilityValue_descriptor,
        new java.lang.String[] { "Value", });
    internal_static_facebook_remote_execution_WorkerRequirements_descriptor =
      getDescriptor().getMessageTypes().get(9);
    internal_static_facebook_remote_execution_WorkerRequirements_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_WorkerRequirements_descriptor,
        new java.lang.String[] { "WorkerSize", "PlatformType", "ShouldTryLargerWorkerOnOom", "Testing", "TaskName", });
    internal_static_facebook_remote_execution_ClientJobInfo_descriptor =
      getDescriptor().getMessageTypes().get(10);
    internal_static_facebook_remote_execution_ClientJobInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_ClientJobInfo_descriptor,
        new java.lang.String[] { "DeploymentStage", "InstanceId", "GroupId", "ClientSideTenant", });
    internal_static_facebook_remote_execution_ClientActionInfo_descriptor =
      getDescriptor().getMessageTypes().get(11);
    internal_static_facebook_remote_execution_ClientActionInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_ClientActionInfo_descriptor,
        new java.lang.String[] { "Repository", "ScheduleType", "ReSessionLabel", "TenantId", });
    internal_static_facebook_remote_execution_ExecutedActionInfo_descriptor =
      getDescriptor().getMessageTypes().get(12);
    internal_static_facebook_remote_execution_ExecutedActionInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_ExecutedActionInfo_descriptor,
        new java.lang.String[] { "CpuStatUsageUsec", "CpuStatUserUsec", "CpuStatSystemUsec", "IsFallbackEnabledForCompletedAction", "EngineScheduledCount", "MaxUsedMem", "HostTotalMem", "TaskTotalMem", "ExecutedOnElasticCapacity", "Deduplicated", });
    internal_static_facebook_remote_execution_DebugInfo_descriptor =
      getDescriptor().getMessageTypes().get(13);
    internal_static_facebook_remote_execution_DebugInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_DebugInfo_descriptor,
        new java.lang.String[] { "PauseBeforeCleanTimeout", });
    internal_static_facebook_remote_execution_RemoteExecutionMetadata_descriptor =
      getDescriptor().getMessageTypes().get(14);
    internal_static_facebook_remote_execution_RemoteExecutionMetadata_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_facebook_remote_execution_RemoteExecutionMetadata_descriptor,
        new java.lang.String[] { "ReSessionId", "BuckInfo", "TraceInfo", "CreatorInfo", "EngineInfo", "WorkerInfo", "CasClientInfo", "WorkerRequirements", "ClientActionInfo", "ExecutedActionInfo", "DebugInfo", "ClientJobInfo", });
    com.google.protobuf.WrappersProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
