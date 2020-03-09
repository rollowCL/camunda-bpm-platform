/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
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
package org.camunda.bpm.engine.impl.migration.batch;

import org.camunda.bpm.engine.batch.Batch;
import org.camunda.bpm.engine.impl.batch.AbstractBatchJobHandler;
import org.camunda.bpm.engine.impl.batch.BatchJobConfiguration;
import org.camunda.bpm.engine.impl.batch.BatchJobContext;
import org.camunda.bpm.engine.impl.batch.BatchJobDeclaration;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.jobexecutor.JobDeclaration;
import org.camunda.bpm.engine.impl.json.MigrationBatchConfigurationJsonConverter;
import org.camunda.bpm.engine.impl.migration.MigrationPlanExecutionBuilderImpl;
import org.camunda.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.MessageEntity;
import org.camunda.bpm.engine.migration.MigrationPlanExecutionBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Job handler for batch migration jobs. The batch migration job
 * migrates a list of process instances.
 */
public class MigrationBatchJobHandler extends AbstractBatchJobHandler<MigrationBatchConfiguration> {

  public static final BatchJobDeclaration JOB_DECLARATION = new BatchJobDeclaration(Batch.TYPE_PROCESS_INSTANCE_MIGRATION);

  public String getType() {
    return Batch.TYPE_PROCESS_INSTANCE_MIGRATION;
  }

  public JobDeclaration<BatchJobContext, MessageEntity> getJobDeclaration() {
    return JOB_DECLARATION;
  }

  protected MigrationBatchConfigurationJsonConverter getJsonConverterInstance() {
    return MigrationBatchConfigurationJsonConverter.INSTANCE;
  }

  @Override
  protected MigrationBatchConfiguration createJobConfiguration(MigrationBatchConfiguration configuration, List<String> processIdsForJob) {
    return new MigrationBatchConfiguration(
        processIdsForJob,
        configuration.getMigrationPlan(),
        configuration.isSkipCustomListeners(),
        configuration.isSkipIoMappings()
    );
  }

  @Override
  protected Map<String, List<String>> getProcessIdsPerDeployment(CommandContext commandContext, List<String> processIds, MigrationBatchConfiguration configuration) {
    String sourceProcessDefinitionId = configuration.getMigrationPlan().getSourceProcessDefinitionId();
    String deploymentId = commandContext.getProcessEngineConfiguration()
      .getDeploymentCache().findDeployedProcessDefinitionById(sourceProcessDefinitionId)
      .getDeploymentId();
    return Collections.singletonMap(deploymentId, processIds);
  }

  @Override
  public void execute(BatchJobConfiguration configuration, ExecutionEntity execution, CommandContext commandContext, String tenantId) {
    ByteArrayEntity configurationEntity = commandContext
        .getDbEntityManager()
        .selectById(ByteArrayEntity.class, configuration.getConfigurationByteArrayId());

    MigrationBatchConfiguration batchConfiguration = readConfiguration(configurationEntity.getBytes());

    MigrationPlanExecutionBuilder executionBuilder = commandContext.getProcessEngineConfiguration()
        .getRuntimeService()
        .newMigration(batchConfiguration.getMigrationPlan())
        .processInstanceIds(batchConfiguration.getIds());

    if (batchConfiguration.isSkipCustomListeners()) {
      executionBuilder.skipCustomListeners();
    }
    if (batchConfiguration.isSkipIoMappings()) {
      executionBuilder.skipIoMappings();
    }

    // uses internal API in order to skip writing user operation log (CommandContext#disableUserOperationLog
    // is not sufficient with legacy engine config setting "restrictUserOperationLogToAuthenticatedUsers" = false)
    ((MigrationPlanExecutionBuilderImpl) executionBuilder).execute(false);

    commandContext.getByteArrayManager().delete(configurationEntity);
  }

}
