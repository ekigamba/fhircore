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

package org.smartregister.fhircore.engine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor

const val DOT_TAG = "dotTag"

@Composable
fun Dot(modifier: Modifier = Modifier, showDot: Boolean = true) {
  if (showDot) {
    Spacer(modifier = modifier.width(8.dp))
    Box(
      modifier =
        modifier
          .testTag(DOT_TAG)
          .clip(CircleShape)
          .size(2.6.dp)
          .background(color = SubtitleTextColor)
    )
    Spacer(modifier = modifier.width(8.dp))
  }
}
