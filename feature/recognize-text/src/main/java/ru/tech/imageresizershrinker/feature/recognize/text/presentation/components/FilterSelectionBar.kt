/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2024 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package ru.tech.imageresizershrinker.feature.recognize.text.presentation.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.tech.imageresizershrinker.core.filters.domain.model.Filter
import ru.tech.imageresizershrinker.core.resources.R
import ru.tech.imageresizershrinker.core.ui.widget.modifier.container

@Composable
fun FilterSelectionBar(
    addedFilters: List<Filter<Bitmap, *>>,
    onContrastClick: () -> Unit,
    onThresholdClick: () -> Unit,
    onSharpnessClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val (hasContrast, hasSharpness, hasThreshold) = remember(addedFilters) {
        derivedStateOf {
            Triple(
                addedFilters.filterIsInstance<Filter.Contrast<Bitmap>>().isNotEmpty(),
                addedFilters.filterIsInstance<Filter.Sharpen<Bitmap>>().isNotEmpty(),
                addedFilters.filterIsInstance<Filter.LuminanceThreshold<Bitmap>>().isNotEmpty()
            )
        }
    }.value

    val thresholdColor by animateColorAsState(
        if (hasThreshold) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow
    )

    val contrastColor by animateColorAsState(
        if (hasContrast) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow
    )

    val sharpnessColor by animateColorAsState(
        if (hasSharpness) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.container(
            RoundedCornerShape(24.dp)
        )
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.transformations),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .container(
                        color = contrastColor,
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            bottomStart = 20.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        resultPadding = 0.dp
                    )
                    .clickable {
                        haptics.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        onContrastClick()
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.contrast),
                    color = contentColorFor(contrastColor)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .container(
                        color = sharpnessColor,
                        shape = RoundedCornerShape(4.dp),
                        resultPadding = 0.dp
                    )
                    .clickable {
                        haptics.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        onSharpnessClick()
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.sharpen),
                    color = contentColorFor(sharpnessColor)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .container(
                        color = thresholdColor,
                        shape = RoundedCornerShape(
                            topEnd = 20.dp,
                            bottomEnd = 20.dp,
                            topStart = 4.dp,
                            bottomStart = 4.dp
                        ),
                        resultPadding = 0.dp
                    )
                    .clickable {
                        haptics.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        onThresholdClick()
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.threshold),
                    color = contentColorFor(thresholdColor)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}