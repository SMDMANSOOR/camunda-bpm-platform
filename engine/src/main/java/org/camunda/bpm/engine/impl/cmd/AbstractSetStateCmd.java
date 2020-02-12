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
package org.camunda.bpm.engine.impl.cmd;

import java.util.Date;
import java.util.concurrent.Callable;

import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.SuspensionState;
import org.camunda.bpm.engine.impl.persistence.entity.TimerEntity;

/**
 * @author Roman Smirnov
 *
 */
public abstract class AbstractSetStateCmd implements Command<Void> {

  protected static final String SUSPENSION_STATE_PROPERTY = "suspensionState";

  protected boolean includeSubResources;
  protected boolean isLogUserOperationDisabled;
  protected Date executionDate;

  public AbstractSetStateCmd(boolean includeSubResources, Date executionDate) {
    this.includeSubResources = includeSubResources;
    this.executionDate = executionDate;
  }

  public Void execute(final CommandContext commandContext) {
    checkParameters(commandContext);
    checkAuthorization(commandContext);

    if (executionDate == null) {

      updateSuspensionState(commandContext, getNewSuspensionState());

      if (isIncludeSubResources()) {
        final AbstractSetStateCmd cmd = getNextCommand();
        if (cmd != null) {
          cmd.disableLogUserOperation();
          // avoids unnecessary authorization checks
          // pre-requirement: the necessary authorization check
          // for included resources should be done before this
          // call.
          commandContext.runWithoutAuthorization(new Callable<Void>() {
            public Void call() throws Exception {
              cmd.execute(commandContext);
              return null;
            }
          });
        }
      }

      triggerHistoryEvent(commandContext);
    } else {
      scheduleSuspensionStateUpdate(commandContext);
    }

    if (!isLogUserOperationDisabled()) {
      logUserOperation(commandContext);
    }

    return null;
  }

  protected void triggerHistoryEvent(CommandContext commandContext) {

  }

  public void disableLogUserOperation() {
    this.isLogUserOperationDisabled = true;
  }

  protected boolean isLogUserOperationDisabled() {
    return isLogUserOperationDisabled;
  }

  protected boolean isIncludeSubResources() {
    return includeSubResources;
  }

  protected void scheduleSuspensionStateUpdate(CommandContext commandContext) {
    TimerEntity timer = new TimerEntity();

    JobHandlerConfiguration jobHandlerConfiguration = getJobHandlerConfiguration();

    timer.setDuedate(executionDate);
    timer.setJobHandlerType(getDelayedExecutionJobHandlerType());
    timer.setJobHandlerConfigurationRaw(jobHandlerConfiguration.toCanonicalString());
    timer.setDeploymentId(getDeploymentId(commandContext));

    commandContext.getJobManager().schedule(timer);
  }


  protected String getDelayedExecutionJobHandlerType() {
    return null;
  }

  protected JobHandlerConfiguration getJobHandlerConfiguration() {
    return null;
  }

  protected AbstractSetStateCmd getNextCommand() {
    return null;
  }

  protected abstract void checkAuthorization(CommandContext commandContext);

  protected abstract void checkParameters(CommandContext commandContext);

  protected abstract void updateSuspensionState(CommandContext commandContext, SuspensionState suspensionState);

  protected abstract void logUserOperation(CommandContext commandContext);

  protected abstract String getLogEntryOperation();

  protected abstract SuspensionState getNewSuspensionState();

  protected abstract String getDeploymentId(CommandContext commandContext);

  protected String getDeploymentIdByProcessDefinition(CommandContext commandContext, String processDefinitionId) {
    ProcessDefinitionEntity definition = Context.getProcessEngineConfiguration().getDeploymentCache()
        .findDeployedProcessDefinitionById(processDefinitionId);
    if (definition != null) {
      return definition.getDeploymentId();
    }
    return null;
  }

}
