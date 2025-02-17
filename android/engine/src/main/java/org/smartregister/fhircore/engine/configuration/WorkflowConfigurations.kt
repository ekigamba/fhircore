/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.configuration

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Resource

@Serializable
data class ApplicationWorkflow(
  val appId: String,
  val title: String,
  val mapping: List<WorkflowPoint>
)

@Serializable
data class WorkflowPoint(
  val workflowPoint: String,
  @Contextual val resource: Resource,
  val classification: String,
  val description: String
)
