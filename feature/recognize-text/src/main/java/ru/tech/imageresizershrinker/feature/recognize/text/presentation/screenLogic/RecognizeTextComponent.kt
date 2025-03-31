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

@file:Suppress("FunctionName")

package ru.tech.imageresizershrinker.feature.recognize.text.presentation.screenLogic

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropOutlineProperty
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.tech.imageresizershrinker.core.data.utils.asDomain
import ru.tech.imageresizershrinker.core.data.utils.toCoil
import ru.tech.imageresizershrinker.core.domain.dispatchers.DispatchersHolder
import ru.tech.imageresizershrinker.core.domain.image.ImageGetter
import ru.tech.imageresizershrinker.core.domain.image.ImageScaler
import ru.tech.imageresizershrinker.core.domain.image.ImageTransformer
import ru.tech.imageresizershrinker.core.domain.image.ShareProvider
import ru.tech.imageresizershrinker.core.domain.image.model.ImageInfo
import ru.tech.imageresizershrinker.core.domain.image.model.MetadataTag
import ru.tech.imageresizershrinker.core.domain.model.DomainAspectRatio
import ru.tech.imageresizershrinker.core.domain.resource.ResourceManager
import ru.tech.imageresizershrinker.core.domain.saving.FileController
import ru.tech.imageresizershrinker.core.domain.saving.FilenameCreator
import ru.tech.imageresizershrinker.core.domain.saving.model.FileSaveTarget
import ru.tech.imageresizershrinker.core.domain.saving.model.ImageSaveTarget
import ru.tech.imageresizershrinker.core.domain.saving.model.SaveResult
import ru.tech.imageresizershrinker.core.domain.saving.model.SaveTarget
import ru.tech.imageresizershrinker.core.domain.saving.model.onSuccess
import ru.tech.imageresizershrinker.core.domain.utils.runSuspendCatching
import ru.tech.imageresizershrinker.core.domain.utils.smartJob
import ru.tech.imageresizershrinker.core.filters.domain.FilterProvider
import ru.tech.imageresizershrinker.core.filters.domain.model.Filter
import ru.tech.imageresizershrinker.core.filters.presentation.model.UiContrastFilter
import ru.tech.imageresizershrinker.core.filters.presentation.model.UiSharpenFilter
import ru.tech.imageresizershrinker.core.filters.presentation.model.UiThresholdFilter
import ru.tech.imageresizershrinker.core.resources.R
import ru.tech.imageresizershrinker.core.settings.domain.SettingsManager
import ru.tech.imageresizershrinker.core.ui.utils.BaseComponent
import ru.tech.imageresizershrinker.core.ui.utils.helper.ImageUtils.safeAspectRatio
import ru.tech.imageresizershrinker.core.ui.utils.navigation.Screen
import ru.tech.imageresizershrinker.core.ui.utils.state.update
import ru.tech.imageresizershrinker.feature.recognize.text.domain.DownloadData
import ru.tech.imageresizershrinker.feature.recognize.text.domain.ImageTextReader
import ru.tech.imageresizershrinker.feature.recognize.text.domain.OCRLanguage
import ru.tech.imageresizershrinker.feature.recognize.text.domain.OcrEngineMode
import ru.tech.imageresizershrinker.feature.recognize.text.domain.RecognitionData
import ru.tech.imageresizershrinker.feature.recognize.text.domain.RecognitionType
import ru.tech.imageresizershrinker.feature.recognize.text.domain.SegmentationMode
import ru.tech.imageresizershrinker.feature.recognize.text.domain.TessParams
import ru.tech.imageresizershrinker.feature.recognize.text.domain.TextRecognitionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil3.transform.Transformation as CoilTransformation

class RecognizeTextComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted val initialType: Screen.RecognizeText.Type?,
    @Assisted onGoBack: () -> Unit,
    private val imageGetter: ImageGetter<Bitmap>,
    private val imageTextReader: ImageTextReader<Bitmap>,
    private val settingsManager: SettingsManager,
    private val imageTransformer: ImageTransformer<Bitmap>,
    private val filterProvider: FilterProvider<Bitmap>,
    private val imageScaler: ImageScaler<Bitmap>,
    private val shareProvider: ShareProvider<Bitmap>,
    private val fileController: FileController,
    private val filenameCreator: FilenameCreator,
    resourceManager: ResourceManager,
    dispatchersHolder: DispatchersHolder
) : BaseComponent(dispatchersHolder, componentContext), ResourceManager by resourceManager {
    //TODO: Needs refactor

    private val _segmentationMode: MutableState<SegmentationMode> =
        mutableStateOf(SegmentationMode.PSM_AUTO_OSD)
    val segmentationMode by _segmentationMode

    private val _ocrEngineMode: MutableState<OcrEngineMode> = mutableStateOf(OcrEngineMode.DEFAULT)
    val ocrEngineMode by _ocrEngineMode

    private val _params: MutableState<TessParams> = mutableStateOf(TessParams.Default)
    val params by _params

    private val _selectedLanguages = mutableStateOf(listOf(OCRLanguage.Default))
    val selectedLanguages by _selectedLanguages

    private var isRecognitionTypeSet = false
    private val _recognitionType = mutableStateOf(RecognitionType.Standard)
    val recognitionType by _recognitionType

    private val _type = mutableStateOf<Screen.RecognizeText.Type?>(null)
    val type by _type

    val uris: List<Uri>
        get() = when (val target = type) {
            is Screen.RecognizeText.Type.WriteToFile -> target.uris ?: emptyList()
            is Screen.RecognizeText.Type.WriteToMetadata -> target.uris ?: emptyList()
            else -> emptyList()
        }

    val onGoBack: () -> Unit = {
        if (type == null) onGoBack()
        else _type.update { null }
    }

    private val _recognitionData = mutableStateOf<RecognitionData?>(null)
    val recognitionData by _recognitionData

    private val _textLoadingProgress: MutableState<Int> = mutableIntStateOf(-1)
    val textLoadingProgress by _textLoadingProgress

    private val _languages: MutableState<List<OCRLanguage>> = mutableStateOf(emptyList())
    val languages by _languages

    private val contrastFilterInstance = UiContrastFilter()

    private val sharpenFilterInstance = UiSharpenFilter()

    private val thresholdFilterInstance = UiThresholdFilter()

    private val filtersOrder = listOf(
        contrastFilterInstance,
        sharpenFilterInstance,
        thresholdFilterInstance
    )

    private val _filtersAdded: MutableState<List<Filter<*>>> = mutableStateOf(emptyList())
    val filtersAdded by _filtersAdded

    private val internalBitmap: MutableState<Bitmap?> = mutableStateOf(null)

    private val _previewBitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val previewBitmap: Bitmap? by _previewBitmap

    private val _rotation: MutableState<Float> = mutableFloatStateOf(0f)

    private val _isFlipped: MutableState<Boolean> = mutableStateOf(false)

    private val _selectedAspectRatio: MutableState<DomainAspectRatio> =
        mutableStateOf(DomainAspectRatio.Free)
    val selectedAspectRatio by _selectedAspectRatio

    private val _cropProperties = mutableStateOf(
        CropDefaults.properties(
            cropOutlineProperty = CropOutlineProperty(
                OutlineType.Rect,
                RectCropShape(
                    id = 0,
                    title = OutlineType.Rect.name
                )
            ),
            fling = true
        )
    )
    val cropProperties by _cropProperties

    private val _isExporting: MutableState<Boolean> = mutableStateOf(false)
    val isExporting by _isExporting

    private val _done: MutableState<Int> = mutableIntStateOf(0)
    val done by _done

    private val _left: MutableState<Int> = mutableIntStateOf(-1)
    val left by _left

    private val _isSaving: MutableState<Boolean> = mutableStateOf(false)
    val isSaving by _isSaving

    private var languagesJob: Job? by smartJob {
        _isExporting.update { false }
    }

    val isTextLoading: Boolean
        get() = textLoadingProgress in 0..100

    private var loadingJob: Job? by smartJob()

    private val _isSelectionTypeSheetVisible = mutableStateOf(false)
    val isSelectionTypeSheetVisible by _isSelectionTypeSheetVisible

    fun showSelectionTypeSheet() {
        _isSelectionTypeSheetVisible.update { true }
    }

    fun hideSelectionTypeSheet() {
        _isSelectionTypeSheetVisible.update { false }
    }

    private fun loadLanguages(
        onComplete: suspend () -> Unit = {}
    ) {
        loadingJob = componentScope.launch {
            delay(200L)
            if (!isRecognitionTypeSet) {
                _recognitionType.update {
                    RecognitionType.entries[settingsManager.getInitialOcrMode()]
                }
                isRecognitionTypeSet = true
            }
            val data = imageTextReader.getLanguages(recognitionType)
            _selectedLanguages.update { ocrLanguages ->
                val list = ocrLanguages.toMutableList()
                data.forEach { ocrLanguage ->
                    ocrLanguages.indexOfFirst {
                        it.code == ocrLanguage.code
                    }.takeIf { it != -1 }?.let { index ->
                        list[index] = ocrLanguage
                    }
                }
                list
            }
            _languages.update { data }
            onComplete()
        }
    }

    init {
        loadLanguages()
        componentScope.launch {
            val languageCodes = settingsManager.getInitialOCRLanguageCodes().map {
                imageTextReader.getLanguageForCode(it)
            }
            _selectedLanguages.update { languageCodes }
        }
    }

    fun getTransformations(): List<CoilTransformation> = filtersOrder.filter {
        it in filtersAdded
    }.map {
        filterProvider.filterToTransformation(it).toCoil()
    }

    fun updateType(
        type: Screen.RecognizeText.Type?,
        onImageSet: () -> Unit
    ) {
        type?.let {
            componentScope.launch {
                _isImageLoading.value = true
                _type.update { type }
                if (type is Screen.RecognizeText.Type.Extraction) {
                    imageGetter.getImage(
                        data = type.uri ?: "",
                        originalSize = false
                    )?.let {
                        updateBitmap(it, onImageSet)
                    }
                }
                _isImageLoading.value = false
            }
        }
    }

    fun save(
        oneTimeSaveLocationUri: String?,
        onResult: (List<SaveResult>) -> Unit,
        onRequestDownload: (List<DownloadData>) -> Unit,
    ) {
        recognitionJob = componentScope.launch {
            delay(400)
            _isSaving.update { true }
            when (type) {
                is Screen.RecognizeText.Type.WriteToFile -> {
                    val txtString = StringBuilder()

                    _left.update { uris.size }

                    uris.forEach { uri ->
                        imageTextReader.getTextFromImage(
                            type = recognitionType,
                            languageCode = selectedLanguages.joinToString("+") { it.code },
                            segmentationMode = segmentationMode,
                            image = imageGetter.getImage(uri)?.let { bitmap ->
                                imageTransformer.transform(
                                    transformations = getTransformations().map(CoilTransformation::asDomain),
                                    image = bitmap
                                )
                            },
                            parameters = params,
                            ocrEngineMode = ocrEngineMode,
                            onProgress = { }
                        ).also { result ->
                            result.appendToStringBuilder(
                                builder = txtString,
                                uri = uri,
                                onRequestDownload = {
                                    onRequestDownload(it)
                                    return@launch
                                }
                            )
                            _done.update { it + 1 }
                        }
                    }

                    onResult(
                        listOf(
                            fileController.save(
                                saveTarget = TxtSaveTarget(
                                    txtBytes = txtString.toString().toByteArray()
                                ),
                                keepOriginalMetadata = true,
                                oneTimeSaveLocationUri = oneTimeSaveLocationUri
                            ).onSuccess(::registerSave)
                        )
                    )
                }

                is Screen.RecognizeText.Type.WriteToMetadata -> {
                    val results = mutableListOf<SaveResult>()

                    _left.update { uris.size }

                    uris.forEach { uri ->
                        runSuspendCatching {
                            imageGetter.getImage(uri.toString())
                        }.getOrNull()?.let {
                            imageTextReader.getTextFromImage(
                                type = recognitionType,
                                languageCode = selectedLanguages.joinToString("+") { it.code },
                                segmentationMode = segmentationMode,
                                image = imageGetter.getImage(uri)?.let { bitmap ->
                                    imageTransformer.transform(
                                        transformations = getTransformations().map(
                                            CoilTransformation::asDomain
                                        ),
                                        image = bitmap
                                    )
                                },
                                parameters = params,
                                ocrEngineMode = ocrEngineMode,
                                onProgress = { }
                            ).also { result ->
                                val txtString = when (result) {
                                    is TextRecognitionResult.Error -> {
                                        result.throwable.message ?: ""
                                    }

                                    is TextRecognitionResult.NoData -> {
                                        onRequestDownload(result.data)
                                        return@launch
                                    }

                                    is TextRecognitionResult.Success -> {
                                        result.data.text.ifEmpty { getString(R.string.picture_has_no_text) }
                                    }
                                }

                                results.add(
                                    fileController.save(
                                        ImageSaveTarget(
                                            imageInfo = it.imageInfo,
                                            originalUri = uri.toString(),
                                            sequenceNumber = null,
                                            metadata = it.metadata?.apply {
                                                setAttribute(
                                                    MetadataTag.UserComment,
                                                    txtString.takeIf { it.isNotEmpty() }
                                                )
                                            },
                                            data = ByteArray(0),
                                            readFromUriInsteadOfData = true
                                        ),
                                        keepOriginalMetadata = false,
                                        oneTimeSaveLocationUri = oneTimeSaveLocationUri
                                    )
                                )
                                _done.update { it + 1 }
                            }
                        }
                    }

                    onResult(results.onSuccess(::registerSave))
                }

                else -> return@launch
            }
        }.apply {
            invokeOnCompletion {
                _isSaving.update { false }
            }
        }
    }

    private fun TxtSaveTarget(
        txtBytes: ByteArray
    ): SaveTarget = FileSaveTarget(
        originalUri = "",
        filename = filenameCreator.constructImageFilename(
            ImageSaveTarget(
                imageInfo = ImageInfo(),
                originalUri = "",
                sequenceNumber = null,
                metadata = null,
                data = ByteArray(0),
                extension = "txt"
            ),
            oneTimePrefix = "OCR_images(${uris.size})",
            forceNotAddSizeInFilename = true
        ),
        data = txtBytes,
        mimeType = "text/plain",
        extension = "txt"
    )

    fun removeUri(uri: Uri) {
        when (type) {
            is Screen.RecognizeText.Type.WriteToFile -> {
                updateType(
                    type = Screen.RecognizeText.Type.WriteToFile(uris - uri),
                    onImageSet = {}
                )
            }

            is Screen.RecognizeText.Type.WriteToMetadata -> {
                updateType(
                    type = Screen.RecognizeText.Type.WriteToMetadata(uris - uri),
                    onImageSet = {}
                )
            }

            else -> Unit
        }
    }

    fun updateBitmap(
        bitmap: Bitmap,
        onComplete: () -> Unit = {}
    ) {
        componentScope.launch {
            _isImageLoading.value = true
            _previewBitmap.value = imageScaler.scaleUntilCanShow(bitmap)
            internalBitmap.update { previewBitmap }
            _isImageLoading.value = false
            onComplete()
        }
    }

    private var recognitionJob: Job? by smartJob {
        _textLoadingProgress.update { -1 }
        _isSaving.update { false }
    }

    fun startRecognition(
        onFailure: (Throwable) -> Unit,
        onRequestDownload: (List<DownloadData>) -> Unit
    ) {
        recognitionJob = componentScope.launch {
            if (_type.value !is Screen.RecognizeText.Type.Extraction) return@launch
            delay(400L)
            _textLoadingProgress.update { 0 }
            imageTextReader.getTextFromImage(
                type = recognitionType,
                languageCode = selectedLanguages.joinToString("+") { it.code },
                segmentationMode = segmentationMode,
                image = previewBitmap?.let { bitmap ->
                    imageTransformer.transform(
                        transformations = getTransformations().map {
                            it.asDomain()
                        },
                        image = bitmap
                    )
                },
                parameters = params,
                ocrEngineMode = ocrEngineMode,
                onProgress = { progress ->
                    _textLoadingProgress.update { progress }
                }
            ).also { result ->
                when (result) {
                    is TextRecognitionResult.Error -> {
                        onFailure(result.throwable)
                    }

                    is TextRecognitionResult.NoData -> {
                        onRequestDownload(result.data)
                    }

                    is TextRecognitionResult.Success -> {
                        _recognitionData.update { result.data }
                    }
                }
            }
            _textLoadingProgress.update { -1 }
        }
    }

    fun setRecognitionType(recognitionType: RecognitionType) {
        _recognitionType.update { recognitionType }
        componentScope.launch {
            settingsManager.setInitialOcrMode(recognitionType.ordinal)
        }
        loadLanguages()
    }

    private val downloadMutex = Mutex()
    fun downloadTrainData(
        type: RecognitionType,
        languageCode: String,
        onProgress: (Float, Long) -> Unit,
        onComplete: () -> Unit
    ) {
        componentScope.launch {
            downloadMutex.withLock {
                imageTextReader.downloadTrainingData(
                    type = type,
                    languageCode = languageCode,
                    onProgress = onProgress
                )
                loadLanguages {
                    settingsManager.setInitialOCRLanguageCodes(
                        selectedLanguages.filter {
                            it.downloaded.isNotEmpty()
                        }.map { it.code }
                    )
                }
                onComplete()
            }
        }
    }

    fun onLanguagesSelected(ocrLanguages: List<OCRLanguage>) {
        if (ocrLanguages.isNotEmpty()) {
            componentScope.launch {
                settingsManager.setInitialOCRLanguageCodes(
                    ocrLanguages.filter {
                        it.downloaded.isNotEmpty()
                    }.map { it.code }
                )
            }
            _selectedLanguages.update { ocrLanguages }
            _recognitionData.update { null }
            recognitionJob?.cancel()
            _textLoadingProgress.update { -1 }
        }
    }

    fun setSegmentationMode(segmentationMode: SegmentationMode) {
        _segmentationMode.update { segmentationMode }
    }

    fun deleteLanguage(
        language: OCRLanguage,
        types: List<RecognitionType>,
        onSuccess: () -> Unit
    ) {
        componentScope.launch {
            imageTextReader.deleteLanguage(language, types)
            onLanguagesSelected(selectedLanguages - language)
            val availableTypes = language.downloaded - types.toSet()
            availableTypes.firstOrNull()?.let(::setRecognitionType) ?: loadLanguages()
            onSuccess()
        }
    }

    fun rotateBitmapLeft() {
        _rotation.update { it - 90f }
        debouncedImageCalculation {
            checkBitmapAndUpdate()
        }
    }

    fun rotateBitmapRight() {
        _rotation.update { it + 90f }
        debouncedImageCalculation {
            checkBitmapAndUpdate()
        }
    }

    fun flipImage() {
        _isFlipped.update { !it }
        debouncedImageCalculation {
            checkBitmapAndUpdate()
        }
    }

    private suspend fun checkBitmapAndUpdate() {
        _previewBitmap.value = internalBitmap.value?.let {
            imageTransformer.flip(
                image = imageTransformer.rotate(
                    image = it,
                    degrees = _rotation.value
                ),
                isFlipped = _isFlipped.value
            )
        }
    }

    fun setCropAspectRatio(
        domainAspectRatio: DomainAspectRatio,
        aspectRatio: AspectRatio
    ) {
        _cropProperties.update { properties ->
            properties.copy(
                aspectRatio = aspectRatio.takeIf {
                    domainAspectRatio != DomainAspectRatio.Original
                } ?: _previewBitmap.value?.let {
                    AspectRatio(it.safeAspectRatio)
                } ?: aspectRatio,
                fixedAspectRatio = domainAspectRatio != DomainAspectRatio.Free
            )
        }
        _selectedAspectRatio.update { domainAspectRatio }
    }

    fun setCropMask(cropOutlineProperty: CropOutlineProperty) {
        _cropProperties.value =
            _cropProperties.value.copy(cropOutlineProperty = cropOutlineProperty)
    }

    suspend fun loadImage(uri: Uri): Bitmap? = imageGetter.getImage(data = uri)

    fun toggleContrastFilter() {
        _filtersAdded.update {
            if (contrastFilterInstance in it) it - contrastFilterInstance
            else it + contrastFilterInstance
        }
    }

    fun toggleThresholdFilter() {
        _filtersAdded.update {
            if (thresholdFilterInstance in it) it - thresholdFilterInstance
            else it + thresholdFilterInstance
        }
    }

    fun toggleSharpnessFilter() {
        _filtersAdded.update {
            if (sharpenFilterInstance in it) it - sharpenFilterInstance
            else it + sharpenFilterInstance
        }
    }

    fun setOcrEngineMode(mode: OcrEngineMode) {
        _ocrEngineMode.update { mode }
    }

    fun shareText(
        text: String?,
        onComplete: () -> Unit
    ) {
        text?.let {
            shareProvider.shareText(
                value = it,
                onComplete = onComplete
            )
        }
    }

    fun shareData(
        onComplete: () -> Unit,
        onRequestDownload: (List<DownloadData>) -> Unit
    ) {
        recognitionJob = componentScope.launch {
            delay(400)
            _isSaving.update { true }
            when (type) {
                is Screen.RecognizeText.Type.WriteToFile -> {
                    val txtString = StringBuilder()

                    _left.update { uris.size }

                    uris.forEach { uri ->
                        imageTextReader.getTextFromImage(
                            type = recognitionType,
                            languageCode = selectedLanguages.joinToString("+") { it.code },
                            segmentationMode = segmentationMode,
                            image = imageGetter.getImage(uri)?.let { bitmap ->
                                imageTransformer.transform(
                                    transformations = getTransformations().map(CoilTransformation::asDomain),
                                    image = bitmap
                                )
                            },
                            parameters = params,
                            ocrEngineMode = ocrEngineMode,
                            onProgress = { }
                        ).also { result ->
                            result.appendToStringBuilder(
                                builder = txtString,
                                uri = uri,
                                onRequestDownload = {
                                    onRequestDownload(it)
                                    return@launch
                                }
                            )
                            _done.update { it + 1 }
                        }
                    }

                    val saveTarget = TxtSaveTarget(
                        txtBytes = txtString.toString().toByteArray()
                    )

                    shareProvider.shareByteArray(
                        byteArray = saveTarget.data,
                        filename = saveTarget.filename ?: "",
                        onComplete = onComplete
                    )
                }

                is Screen.RecognizeText.Type.WriteToMetadata -> {
                    val cachedUris = mutableListOf<String>()

                    _left.update { uris.size }

                    uris.forEach { uri ->
                        runSuspendCatching {
                            imageGetter.getImage(uri.toString())
                        }.getOrNull()?.let {
                            imageTextReader.getTextFromImage(
                                type = recognitionType,
                                languageCode = selectedLanguages.joinToString("+") { it.code },
                                segmentationMode = segmentationMode,
                                image = imageGetter.getImage(uri)?.let { bitmap ->
                                    imageTransformer.transform(
                                        transformations = getTransformations().map(
                                            CoilTransformation::asDomain
                                        ),
                                        image = bitmap
                                    )
                                },
                                parameters = params,
                                ocrEngineMode = ocrEngineMode,
                                onProgress = { }
                            ).also { result ->
                                val txtString = when (result) {
                                    is TextRecognitionResult.Error -> {
                                        result.throwable.message ?: ""
                                    }

                                    is TextRecognitionResult.NoData -> {
                                        onRequestDownload(result.data)
                                        return@launch
                                    }

                                    is TextRecognitionResult.Success -> {
                                        result.data.text.ifEmpty { getString(R.string.picture_has_no_text) }
                                    }
                                }

                                val exif = it.metadata?.apply {
                                    setAttribute(
                                        MetadataTag.UserComment,
                                        txtString.takeIf { it.isNotEmpty() }
                                    )
                                }

                                shareProvider.cacheData(
                                    writeData = { w ->
                                        w.writeBytes(
                                            fileController.readBytes(uri.toString())
                                        )
                                    },
                                    filename = filenameCreator.constructImageFilename(
                                        saveTarget = ImageSaveTarget(
                                            imageInfo = it.imageInfo.copy(originalUri = uri.toString()),
                                            originalUri = uri.toString(),
                                            metadata = exif,
                                            sequenceNumber = null,
                                            data = ByteArray(0)
                                        )
                                    )
                                )?.let { uri ->
                                    fileController.writeMetadata(
                                        imageUri = uri,
                                        metadata = exif
                                    )

                                    cachedUris.add(uri)
                                }

                                _done.update { it + 1 }
                            }
                        }
                    }

                    shareProvider.shareUris(cachedUris)
                }

                else -> return@launch
            }
        }.apply {
            invokeOnCompletion {
                _isSaving.update { false }
            }
        }
    }

    private inline fun TextRecognitionResult.appendToStringBuilder(
        builder: StringBuilder,
        uri: Uri,
        onRequestDownload: (List<DownloadData>) -> Unit
    ) {
        when (this) {
            is TextRecognitionResult.Error -> {
                builder.apply {
                    append("${done + 1} - ")
                    append("[${filenameCreator.getFilename(uri.toString())}]")
                    append("\n\n")
                    append(throwable.message)
                    append("\n\n")
                }
            }

            is TextRecognitionResult.NoData -> onRequestDownload(data)

            is TextRecognitionResult.Success -> {
                builder.apply {
                    append("${done + 1} - ")
                    append("[${filenameCreator.getFilename(uri.toString())}]")
                    append(" ")
                    append(getString(R.string.accuracy, data.accuracy))
                    append("\n\n")
                    append(data.text.ifEmpty { getString(R.string.picture_has_no_text) })
                    append("\n\n")
                }
            }
        }
    }

    fun exportLanguagesTo(
        uri: Uri,
        onResult: (SaveResult) -> Unit
    ) {
        languagesJob = componentScope.launch {
            _isExporting.value = true
            imageTextReader.exportLanguagesToZip()?.let { zipUri ->
                fileController.writeBytes(
                    uri = uri.toString(),
                    block = { it.writeBytes(fileController.readBytes(zipUri)) }
                ).also(onResult).onSuccess(::registerSave)
                _isExporting.value = false
            }
        }
    }

    fun generateExportFilename(): String {
        val timeStamp = SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss",
            Locale.getDefault()
        ).format(Date())
        return "image_toolbox_ocr_languages_$timeStamp.zip"
    }

    fun generateTextFilename(): String {
        val timeStamp = SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss",
            Locale.getDefault()
        ).format(Date())
        return "OCR_$timeStamp.txt"
    }

    fun importLanguagesFrom(
        uri: Uri,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        languagesJob = componentScope.launch {
            _isExporting.value = true
            imageTextReader.importLanguagesFromUri(uri.toString())
                .onSuccess {
                    loadLanguages {
                        onSuccess()
                    }
                }
                .onFailure(onFailure)
            _isExporting.value = false
        }
    }

    fun saveContentToTxt(
        uri: Uri,
        onResult: (SaveResult) -> Unit
    ) {
        recognitionData?.text?.takeIf { it.isNotEmpty() }?.let { data ->
            componentScope.launch {
                fileController.writeBytes(
                    uri = uri.toString(),
                    block = {
                        it.writeBytes(data.encodeToByteArray())
                    }
                ).also(onResult).onSuccess(::registerSave)
            }
        }
    }

    fun updateParams(newParams: TessParams) {
        _params.update { newParams }
    }

    fun cancelSaving() {
        recognitionJob?.cancel()
        recognitionJob = null
        _isSaving.update { false }
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            initialType: Screen.RecognizeText.Type?,
            onGoBack: () -> Unit,
        ): RecognizeTextComponent
    }

}