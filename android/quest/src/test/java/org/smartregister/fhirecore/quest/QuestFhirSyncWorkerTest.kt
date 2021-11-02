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

package org.smartregister.fhirecore.quest

import androidx.work.WorkerParameters
import androidx.work.impl.utils.SerialExecutor
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.Executors
import junit.framework.Assert.assertNotNull
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.quest.QuestFhirSyncWorker
import org.smartregister.fhirecore.quest.robolectric.RobolectricTest
import org.smartregister.fhirecore.quest.shadow.QuestApplicationShadow

@Config(shadows = [QuestApplicationShadow::class])
class QuestFhirSyncWorkerTest : RobolectricTest() {

  private val questSyncWorker by lazy {
    val workParam =
      mockk<WorkerParameters> {
        every { taskExecutor } returns
          mockk {
            every { backgroundExecutor } returns SerialExecutor(Executors.newSingleThreadExecutor())
          }
      }
    QuestFhirSyncWorker(mockk(), workParam)
  }

  @Test
  fun testGetSyncDataShouldReturnNonNullResourceSyncParams() {
    assertNotNull(questSyncWorker.getSyncData())
  }

  @Test
  fun testGetDataSourceShouldReturnNonNullFhirResourceDataSource() {
    assertNotNull(questSyncWorker.getDataSource())
  }

  @Test
  fun testGetFhirEngineShouldReturnNonNullFhirEngine() {
    assertNotNull(questSyncWorker.getFhirEngine())
  }
}