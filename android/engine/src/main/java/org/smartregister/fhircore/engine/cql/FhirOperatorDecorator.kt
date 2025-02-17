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

package org.smartregister.fhircore.engine.cql

import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.MeasureReport

@Singleton
class FhirOperatorDecorator
@Inject
constructor(private val fhirEngine: FhirEngine, private val fhirContext: FhirContext) {

  var operator: FhirOperator? = null

  private fun init() {
    if (operator != null) return
    operator = FhirOperator(fhirContext = fhirContext, fhirEngine = fhirEngine)
  }

  fun evaluateMeasure(
    url: String,
    start: String,
    end: String,
    reportType: String,
    subject: String?,
    practitioner: String?
  ): MeasureReport {
    init()
    return operator!!.evaluateMeasure(url, start, end, reportType, subject, practitioner)
  }

  fun loadLib(lib: Library) {
    init()
    operator!!.loadLib(lib)
  }
}
