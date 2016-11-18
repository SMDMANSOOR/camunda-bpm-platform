/*
 * Copyright 2016 camunda services GmbH.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.camunda.bpm.engine.impl.migration.validation.instruction;

import org.camunda.bpm.engine.impl.bpmn.behavior.BoundaryEventActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.behavior.ConditionalEventBehavior;
import org.camunda.bpm.engine.impl.bpmn.behavior.EventSubProcessStartEventActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class ConditionalEventUpdateEventTriggerValidator implements MigrationInstructionValidator {

  @Override
  public void validate(ValidatingMigrationInstruction instruction,
                       ValidatingMigrationInstructions instructions,
                       MigrationInstructionValidationReportImpl report) {
    ActivityImpl sourceActivity = instruction.getSourceActivity();

    if (isEvent(sourceActivity)
      && sourceActivity.getActivityBehavior() instanceof ConditionalEventBehavior
      && !instruction.isUpdateEventTrigger()) {

      report.addFailure("Conditional event has to migrate with update event trigger.");
    }
  }

  protected boolean isEvent(ActivityImpl activity) {
    ActivityBehavior behavior = activity.getActivityBehavior();
    return behavior instanceof BoundaryEventActivityBehavior
      || behavior instanceof EventSubProcessStartEventActivityBehavior;
  }
}
