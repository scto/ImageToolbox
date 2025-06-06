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

package com.t8rin.imagetoolbox.core.resources.icons


import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Rounded.DashedLine: ImageVector by lazy {
    ImageVector.Builder(
        name = "DashedLine",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(11.291f, 14.828f)
            lineToRelative(-4.242f, 4.242f)
            curveToRelative(-0.585f, 0.585f, -1.534f, 0.585f, -2.119f, 0f)
            lineToRelative(-0f, -0f)
            curveToRelative(-0.585f, -0.585f, -0.585f, -1.534f, 0f, -2.119f)
            lineToRelative(4.242f, -4.242f)
            curveToRelative(0.585f, -0.585f, 1.534f, -0.585f, 2.119f, 0f)
            lineToRelative(0f, 0f)
            curveTo(11.877f, 13.294f, 11.877f, 14.243f, 11.291f, 14.828f)
            close()
        }
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(19.071f, 7.049f)
            lineToRelative(-4.242f, 4.242f)
            curveToRelative(-0.585f, 0.585f, -1.534f, 0.585f, -2.119f, 0f)
            lineToRelative(-0f, -0f)
            curveToRelative(-0.585f, -0.585f, -0.585f, -1.534f, 0f, -2.119f)
            lineToRelative(4.242f, -4.242f)
            curveToRelative(0.585f, -0.585f, 1.534f, -0.585f, 2.119f, 0f)
            lineToRelative(0f, 0f)
            curveTo(19.656f, 5.515f, 19.656f, 6.464f, 19.071f, 7.049f)
            close()
        }
    }.build()
}
