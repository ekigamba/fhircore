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

package org.smartregister.fhircore.eir

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.ToolsVersion
import org.junit.Test
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices

/**
 * Provides a playground for quickly testing and authoring questionnaire.json and the respective
 * StructureMap
 *
 * This should be removed at a later point once we have a more clear way of doing this
 */
class StructureMapTestUtilities : RobolectricTest() {

  @Test
  fun `perform immunization extraction`() {
    val immunizationQuestionnaireResponseString =
      "structure-map-questionnaires/anc/bmi/questionnaire-response-standard.json".readFile()
    val immunizationStructureMap =
      "structure-map-questionnaires/anc/bmi/structure-map.txt".readFile()

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val outputs: MutableList<Base> = ArrayList()
    val transformSupportServices = TransformSupportServices(contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(immunizationStructureMap, "ImmunizationRegistration")

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    val targetResource = Bundle()

    val baseElement =
      iParser.parseResource(
        QuestionnaireResponse::class.java,
        immunizationQuestionnaireResponseString
      )

    scu.transform(contextR4, baseElement, map, targetResource)

    System.out.println(iParser.encodeResourceToString(targetResource))
  }

  @Test
  fun `populate immunization Questionnaire`() {
    val patientJson = "structure-map-questionnaires/eir/immunization/patient.json".readFile()
    val immunizationJson =
      "structure-map-questionnaires/eir/immunization/immunization-1.json".readFile()
    val immunizationStructureMap =
      "structure-map-questionnaires/eir/immunization/structure-map.txt".readFile()

    val questionnaireJson =
      "structure-map-questionnaires/eir/immunization/questionnaire.json".readFile()

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val outputs: MutableList<Base> = ArrayList()
    val transformSupportServices = TransformSupportServices(contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(immunizationStructureMap, "ImmunizationRegistration")

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    val targetResource = Bundle()

    val patient = iParser.parseResource(Patient::class.java, patientJson)
    val immunization = iParser.parseResource(Immunization::class.java, immunizationJson)
    val questionnaire = iParser.parseResource(Questionnaire::class.java, questionnaireJson)

    val questionnaireResponse: QuestionnaireResponse
    runBlocking {
      questionnaireResponse = ResourceMapper.populate(questionnaire, patient, immunization)
    }

    scu.transform(contextR4, questionnaireResponse, map, targetResource)

    System.out.println(iParser.encodeResourceToString(targetResource))
  }

  @Test
  fun `populate patient registration Questionnaire and extract Resources`() {
    val patientRegistrationQuestionnaire =
      "structure-map-questionnaires/eir/patient-registration/questionnaire.json".readFile()
    val patientRegistrationStructureMap =
      "structure-map-questionnaires/eir/patient-registration/structure-map.txt".readFile()
    val relatedPersonJson =
      "structure-map-questionnaires/eir/patient-registration/related-person.json".readFile()
    val patientJson =
      "structure-map-questionnaires/eir/patient-registration/patient.json".readFile()

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val questionnaire =
      iParser.parseResource(Questionnaire::class.java, patientRegistrationQuestionnaire)
    val patient = iParser.parseResource(Patient::class.java, patientJson)
    val relatedPerson = iParser.parseResource(RelatedPerson::class.java, relatedPersonJson)

    var questionnaireResponse: QuestionnaireResponse
    runBlocking {
      questionnaireResponse = ResourceMapper.populate(questionnaire, patient, relatedPerson)
    }

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val outputs: MutableList<Base> = ArrayList()
    val transformSupportServices = TransformSupportServices(contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(patientRegistrationStructureMap, "PatientRegistration")
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    val targetResource = Bundle()

    scu.transform(contextR4, questionnaireResponse, map, targetResource)
    System.out.println(iParser.encodeResourceToString(targetResource))
  }

  @Test
  fun `populate adverse event Questionnaire and extract Resources`() {
    val adverseEventQuestionnaire =
      "structure-map-questionnaires/eir/adverse-event/questionnaire.json".readFile()
    val adverseEventStructureMap =
      "structure-map-questionnaires/eir/adverse-event/structure-map.txt".readFile()
    val immunizationJson =
      "structure-map-questionnaires/eir/adverse-event/immunization.json".readFile()

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val questionnaire = iParser.parseResource(Questionnaire::class.java, adverseEventQuestionnaire)
    val immunization = iParser.parseResource(Immunization::class.java, immunizationJson)

    var questionnaireResponse: QuestionnaireResponse
    runBlocking { questionnaireResponse = ResourceMapper.populate(questionnaire, immunization) }

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val outputs: MutableList<Base> = ArrayList()
    val transformSupportServices = TransformSupportServices(contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(adverseEventStructureMap, "AdverseEvent")
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    val targetResource = Bundle()

    scu.transform(contextR4, questionnaireResponse, map, targetResource)
    System.out.println(iParser.encodeResourceToString(targetResource))
  }

  @Test
  fun `convert StructureMap to JSON`() {
    val patientRegistrationStructureMap =
      "structure-map-questionnaires/eir/patient-registration/structure-map.txt".readFile()
    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))
    contextR4.isCanRunWithoutTerminology = true

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4)
    val map = scu.parse(patientRegistrationStructureMap, "PatientRegistration")

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)
  }

  @Test
  fun `perform extraction from patient registration Questionnaire`() {
    val patientRegistrationQuestionnaireResponse =
      "structure-map-questionnaires/eir/patient-registration/questionnaire-response.json".readFile()
    val patientRegistrationStructureMap =
      "structure-map-questionnaires/eir/patient-registration/structure-map.txt".readFile()

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val outputs: MutableList<Base> = ArrayList()
    val transformSupportServices = TransformSupportServices(contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(patientRegistrationStructureMap, "PatientRegistration")

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    val targetResource = Bundle()

    val baseElement =
      iParser.parseResource(
        QuestionnaireResponse::class.java,
        patientRegistrationQuestionnaireResponse
      )

    scu.transform(contextR4, baseElement, map, targetResource)

    System.out.println(iParser.encodeResourceToString(targetResource))
  }

  @Test
  fun `perform extraction from adverse event Questionnaire`() {
    val adverseEventQuestionnaireResponse =
      "structure-map-questionnaires/eir/adverse-event/questionnaire-response.json".readFile()
    val adverseEventStructureMap =
      "structure-map-questionnaires/eir/adverse-event/structure-map.txt".readFile()

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val outputs: MutableList<Base> = ArrayList()
    val transformSupportServices = TransformSupportServices(contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(adverseEventStructureMap, "AdverseEvent")

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    val targetResource = Bundle()

    val baseElement =
      iParser.parseResource(QuestionnaireResponse::class.java, adverseEventQuestionnaireResponse)

    scu.transform(contextR4, baseElement, map, targetResource)

    System.out.println(iParser.encodeResourceToString(targetResource))
  }

  @Test
  fun `perform extraction from  vital signs metric Questionnaire`() {
    val vitalSignQuestionnaireResponse =
      "structure-map-questionnaires/anc/vital-signs/metric/questionnaire-response-pulse-rate.json".readFile()
    val vitalSignStructureMap =
      "structure-map-questionnaires/anc/vital-signs/metric/structure-map.txt".readFile()

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val outputs: MutableList<Base> = ArrayList()
    val transformSupportServices = TransformSupportServices(contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(vitalSignStructureMap, "VitalSigns")

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    val targetResource = Bundle()

    val baseElement =
      iParser.parseResource(QuestionnaireResponse::class.java, vitalSignQuestionnaireResponse)

    scu.transform(contextR4, baseElement, map, targetResource)

    System.out.println(iParser.encodeResourceToString(targetResource))
  }

  @Test
  fun `perform extraction from  vital signs standard Questionnaire`() {
    val vitalSignQuestionnaireResponse =
      "structure-map-questionnaires/anc/vital-signs/standard/questionnaire-response-pulse-rate.json".readFile()
    val vitalSignStructureMap =
      "structure-map-questionnaires/anc/vital-signs/standard/structure-map.txt".readFile()

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val outputs: MutableList<Base> = ArrayList()
    val transformSupportServices = TransformSupportServices(contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(vitalSignStructureMap, "VitalSigns")

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    val targetResource = Bundle()

    val baseElement =
      iParser.parseResource(QuestionnaireResponse::class.java, vitalSignQuestionnaireResponse)

    scu.transform(contextR4, baseElement, map, targetResource)

    System.out.println(iParser.encodeResourceToString(targetResource))
  }
}
