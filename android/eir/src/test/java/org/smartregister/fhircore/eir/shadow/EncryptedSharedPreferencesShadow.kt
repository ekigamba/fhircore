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

package org.smartregister.fhircore.eir.shadow

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.mockk.mockk
import org.robolectric.annotation.Implements

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 03-12-2021. */
@Implements(EncryptedSharedPreferences::class)
class EncryptedSharedPreferencesShadow {

  companion object {

    @JvmStatic
    fun create(
      context: Context,
      fileName: String,
      masterKey: MasterKey,
      prefKeyEncryptionScheme: EncryptedSharedPreferences.PrefKeyEncryptionScheme,
      prefValueEncryptionScheme: EncryptedSharedPreferences.PrefValueEncryptionScheme
    ): SharedPreferences {
      return mockk<SharedPreferences>()
    }
  }
}
