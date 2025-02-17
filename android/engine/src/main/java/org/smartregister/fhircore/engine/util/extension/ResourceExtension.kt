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

package org.smartregister.fhircore.engine.util.extension

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import com.google.android.fhir.datacapture.common.datatype.asStringValue
import com.google.android.fhir.datacapture.createQuestionnaireResponseItem
import com.google.android.fhir.logicalId
import java.util.Date
import java.util.UUID
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PrimitiveType
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Timing
import org.json.JSONException
import org.json.JSONObject
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import timber.log.Timber

fun Base?.valueToString(): String {
  return if (this == null) return ""
  else if (this.isDateTime) (this as BaseDateTimeType).value.makeItReadable()
  else if (this.isPrimitive) (this as PrimitiveType<*>).asStringValue()
  else if (this is Coding) this.display ?: code
  else if (this is CodeableConcept) this.stringValue()
  else if (this is Quantity) this.value.toPlainString()
  else if (this is Timing)
    this.repeat.let {
      it.period.toPlainString().plus(" ").plus(it.periodUnit.display.capitalize()).plus(" (s)")
    }
  else if (this is HumanName) "${this.given.firstOrNull().valueToString()} ${this.family}"
  else this.toString()
}

fun CodeableConcept.stringValue(): String =
  this.text ?: this.codingFirstRep.display ?: this.codingFirstRep.code

fun Resource.encodeResourceToString(
  parser: IParser = FhirContext.forR4Cached().newJsonParser()
): String = parser.encodeResourceToString(this)

fun <T> String.decodeResourceFromString(
  parser: IParser = FhirContext.forR4Cached().newJsonParser()
): T = parser.parseResource(this) as T

fun <T : Resource> T.updateFrom(updatedResource: Resource): T {
  var extensionUpdateForm = listOf<Extension>()
  if (updatedResource is Patient) {
    extensionUpdateForm = updatedResource.extension
  }
  var extension = listOf<Extension>()
  if (this is Patient) {
    extension = this.extension
  }
  val jsonParser = FhirContext.forR4Cached().newJsonParser()
  val stringJson = encodeResourceToString(jsonParser)
  val originalResourceJson = JSONObject(stringJson)

  originalResourceJson.updateFrom(JSONObject(updatedResource.encodeResourceToString(jsonParser)))
  return jsonParser.parseResource(this::class.java, originalResourceJson.toString()).apply {
    val meta = this.meta
    val metaUpdateForm = this@updateFrom.meta
    if ((meta == null || meta.isEmpty)) {
      if (metaUpdateForm != null) {
        this.meta = metaUpdateForm
        this.meta.tag = metaUpdateForm.tag
      }
    } else {
      val setOfTags = mutableSetOf<Coding>()
      setOfTags.addAll(meta.tag)
      setOfTags.addAll(metaUpdateForm.tag)
      this.meta.tag = setOfTags.distinctBy { it.code + it.system }
    }
    if (this is Patient && this@updateFrom is Patient && updatedResource is Patient) {
      if (extension.isEmpty()) {
        if (extensionUpdateForm.isNotEmpty()) {
          this.extension = extensionUpdateForm
        }
      } else {
        val setOfExtension = mutableSetOf<Extension>()
        setOfExtension.addAll(extension)
        setOfExtension.addAll(extensionUpdateForm)
        this.extension = setOfExtension.distinct()
      }
    }
  }
}

@Throws(JSONException::class)
fun JSONObject.updateFrom(updated: JSONObject) {
  val keys =
    mutableListOf<String>().apply {
      keys().forEach { add(it) }
      updated.keys().forEach { add(it) }
    }

  keys.forEach { key -> updated.opt(key)?.run { put(key, this) } }
}

fun QuestionnaireResponse.generateMissingItems(questionnaire: Questionnaire) =
  questionnaire.item.generateMissingItems(this.item)

fun List<Questionnaire.QuestionnaireItemComponent>.generateMissingItems(
  qrItems: MutableList<QuestionnaireResponse.QuestionnaireResponseItemComponent>
) {
  this.forEachIndexed { index, qItem ->
    // generate complete hierarchy if response item missing otherwise check for nested items
    if (qrItems.isEmpty() || qItem.linkId != qrItems[index].linkId) {
      qrItems.add(index, qItem.createQuestionnaireResponseItem())
    } else qItem.item.generateMissingItems(qrItems[index].item)
  }
}
/**
 * Set all questions that are not of type [Questionnaire.QuestionnaireItemType.GROUP] to readOnly if
 * [readOnly] is true. This also generates the correct FHIRPath population expression for each
 * question when mapped to the corresponding [QuestionnaireResponse]
 */
fun List<Questionnaire.QuestionnaireItemComponent>.prepareQuestionsForReadingOrEditing(
  path: String,
  readOnly: Boolean = false,
) {
  forEach { item ->
    if (item.type != Questionnaire.QuestionnaireItemType.GROUP) {
      item.readOnly = readOnly
      item.item.prepareQuestionsForReadingOrEditing(
        "$path.where(linkId = '${item.linkId}').answer.item",
        readOnly
      )
    } else {
      item.item.prepareQuestionsForReadingOrEditing(
        "$path.where(linkId = '${item.linkId}').item",
        readOnly
      )
    }
  }
}

/** Delete resources in [QuestionnaireResponse.contained] from the database */
suspend fun QuestionnaireResponse.deleteRelatedResources(defaultRepository: DefaultRepository) {
  contained.forEach { defaultRepository.delete(it) }
}

fun QuestionnaireResponse.retainMetadata(questionnaireResponse: QuestionnaireResponse) {
  author = questionnaireResponse.author
  authored = questionnaireResponse.authored
  id = questionnaireResponse.logicalId

  val versionId = Integer.parseInt(questionnaireResponse.meta.versionId ?: "1") + 1

  questionnaireResponse.meta.apply {
    lastUpdated = Date()
    setVersionId(versionId.toString())
  }
}

fun QuestionnaireResponse.assertSubject() {
  if (!this.hasSubject() || !this.subject.hasReference())
    throw IllegalStateException("QuestionnaireResponse must have a subject reference assigned")
}

fun QuestionnaireResponse.getEncounterId(): String? {
  return this.contained
    ?.find { it.resourceType == ResourceType.Encounter }
    ?.logicalId
    ?.replace("#", "")
}

fun Resource.generateMissingId() {
  if (logicalId.isBlank()) id = UUID.randomUUID().toString()
}

fun Resource.isPatient(patientId: String) =
  this.resourceType == ResourceType.Patient && this.logicalId == patientId

fun Resource.asReference(): Reference {
  val referenceValue = "${fhirType()}/$logicalId"

  return Reference().apply { this.reference = referenceValue }
}

fun Resource.referenceValue(): String = "${fhirType()}/$logicalId"

fun Resource.referenceParamForCondition(): ReferenceClientParam =
  when (resourceType) {
    ResourceType.Patient -> Condition.PATIENT
    ResourceType.Encounter -> Condition.ENCOUNTER
    else ->
      throw IllegalStateException("Do not know how to use $resourceType for Condition resource")
  }

fun Resource.referenceParamForObservation(): ReferenceClientParam =
  when (resourceType) {
    ResourceType.Patient -> Observation.PATIENT
    ResourceType.Encounter -> Observation.ENCOUNTER
    ResourceType.QuestionnaireResponse -> Observation.FOCUS
    else ->
      throw IllegalStateException("Do not know how to use $resourceType for Observation resource")
  }

fun Resource.setPropertySafely(name: String, value: Base) =
  kotlin.runCatching { this.setProperty(name, value) }.onFailure { Timber.w(it) }.getOrNull()

fun ResourceType.generateUniqueId() = UUID.randomUUID().toString()
