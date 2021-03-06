/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.core.util;

import org.apache.pinot.common.config.SegmentsValidationAndRetentionConfig;
import org.apache.pinot.common.config.TableConfig;
import org.apache.pinot.common.utils.CommonConstants.Helix.TableType;
import org.apache.pinot.core.realtime.stream.StreamConfig;


/**
 * Methods related to replication
 */
public class ReplicationUtils {

  /**
   * Decides if {@link SegmentsValidationAndRetentionConfig::getReplicationNumber} should be used
   */
  public static boolean useReplication(TableConfig tableConfig) {

    TableType tableType = tableConfig.getTableType();
    if (tableType.equals(TableType.REALTIME)) {
      StreamConfig streamConfig = new StreamConfig(tableConfig.getIndexingConfig().getStreamConfigs());
      return streamConfig.hasHighLevelConsumerType();
    }
    return true;
  }

  /**
   * Decides if {@link SegmentsValidationAndRetentionConfig::getReplicasPerPartitionNumber} should be used
   */
  public static boolean useReplicasPerPartition(TableConfig tableConfig) {

    TableType tableType = tableConfig.getTableType();
    if (tableType.equals(TableType.REALTIME)) {
      StreamConfig streamConfig = new StreamConfig(tableConfig.getIndexingConfig().getStreamConfigs());
      return streamConfig.hasLowLevelConsumerType();
    }
    return false;
  }

  /**
   * Returns the {@link SegmentsValidationAndRetentionConfig::getReplicasPerPartitionNumber} if it is eligible for use,
   * else returns the {@link SegmentsValidationAndRetentionConfig::getReplicationNumber}
   *
   * The reason we have replication as well as replicasPerPartition is, "replication" is used in HLC to support 'split' kafka topics
   * For example, if replication is 3, and we have 6 realtime servers, each server will half of the events in the topic
   * (see @link PinotTableIdealStateBuilder::setupInstanceConfigForHighLevelConsumer}
   * ReplicasPerPartition is used in LLC as the replication.
   * We need to keep both, as we could be operating in dual mode during migrations from HLC to LLC
   */
  public static int getReplication(TableConfig tableConfig) {

    SegmentsValidationAndRetentionConfig validationConfig = tableConfig.getValidationConfig();
    return useReplicasPerPartition(tableConfig) ? validationConfig.getReplicasPerPartitionNumber() : validationConfig
        .getReplicationNumber();
  }

  /**
   * Check if replica groups setup is necessary for realtime
   *
   * Replica groups is supported when only LLC is present.
   * We do not want znode being created or failures in some validations, when attempting to setup replica groups in HLC.
   * As a result, Replica groups cannot be used during migrations from HLC to LLC.
   * In such a scenario, migrate to LLC completely, and then enable replica groups
   *
   */
  public static boolean setupRealtimeReplicaGroups(TableConfig tableConfig) {
    TableType tableType = tableConfig.getTableType();
    if (tableType.equals(TableType.REALTIME)) {
      StreamConfig streamConfig = new StreamConfig(tableConfig.getIndexingConfig().getStreamConfigs());
      return streamConfig.hasLowLevelConsumerType() && !streamConfig.hasHighLevelConsumerType();
    }
    return false;
  }
}
