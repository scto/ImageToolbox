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

package ru.tech.imageresizershrinker.feature.recognize.text.domain

import okhttp3.internal.toImmutableList
import ru.tech.imageresizershrinker.feature.recognize.text.domain.Constants.KEY_CHOP_ENABLE
import ru.tech.imageresizershrinker.feature.recognize.text.domain.Constants.KEY_EDGES_MAX_CHILDREN_PER_OUTLINE
import ru.tech.imageresizershrinker.feature.recognize.text.domain.Constants.KEY_ENABLE_NEW_SEGSEARCH
import ru.tech.imageresizershrinker.feature.recognize.text.domain.Constants.KEY_LANGUAGE_MODEL_NGRAM_ON
import ru.tech.imageresizershrinker.feature.recognize.text.domain.Constants.KEY_PRESERVE_INTERWORD_SPACES
import ru.tech.imageresizershrinker.feature.recognize.text.domain.Constants.KEY_SEGMENT_SEGCOST_RATING
import ru.tech.imageresizershrinker.feature.recognize.text.domain.Constants.KEY_TEXTORD_FORCE_MAKE_PROP_WORDS
import ru.tech.imageresizershrinker.feature.recognize.text.domain.Constants.KEY_USE_NEW_STATE_COST

class TessParam(
    val key: String,
    val value: Any
) {
    val stringValue: String
        get() = when (value) {
            is Boolean -> if (value) "1" else "0"
            else -> value.toString()
        }

    fun copy(value: Any) = TessParam(
        key = key,
        value = value
    )

    override fun equals(other: Any?): Boolean {
        if (other !is TessParam) return false
        if (this.key != other.key) return false

        return this.value == other.value
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    operator fun component1(): String = key
    operator fun component2(): Any = value
}

class TessParams private constructor(
    val tessParamList: List<TessParam>
) {
    fun update(
        key: String,
        transform: (Any) -> Any
    ): TessParams = TessParams(
        tessParamList = tessParamList.toMutableList().apply {
            val index = indexOfFirst { it.key == key }.takeIf {
                it >= 0
            } ?: return this@TessParams

            this[index] = this[index].let {
                it.copy(value = transform(it.value))
            }
        }.toImmutableList()
    )

    companion object {
        val Default by lazy {
            TessParams(
                tessParamList = listOf(
                    TessParam(
                        key = KEY_PRESERVE_INTERWORD_SPACES,
                        value = false
                    ),
                    TessParam(
                        key = KEY_CHOP_ENABLE,
                        value = true
                    ),
                    TessParam(
                        key = KEY_USE_NEW_STATE_COST,
                        value = false
                    ),
                    TessParam(
                        key = KEY_SEGMENT_SEGCOST_RATING,
                        value = false
                    ),
                    TessParam(
                        key = KEY_ENABLE_NEW_SEGSEARCH,
                        value = false
                    ),
                    TessParam(
                        key = KEY_LANGUAGE_MODEL_NGRAM_ON,
                        value = false
                    ),
                    TessParam(
                        key = KEY_TEXTORD_FORCE_MAKE_PROP_WORDS,
                        value = false
                    ),
                    TessParam(
                        key = KEY_EDGES_MAX_CHILDREN_PER_OUTLINE,
                        value = 40
                    )
                )
            )
        }
    }
}