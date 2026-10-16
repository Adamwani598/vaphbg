package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeminiApiClient
import com.example.network.models.*
import com.example.network.service.GeminiFloorPlanAnalysisService
import com.example.network.service.GeminiFloorPlanAnalysisServiceImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray

class BlueprintViewModel(
    private val repository: BlueprintRepository,
    private val floorPlanAnalysisService: GeminiFloorPlanAnalysisService = GeminiApiClient.service
) : ViewModel() {

    // --- Theme & Appearance State ---
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    // --- Dashboard Loading / Skeleton Loader State ---
    private val _isLoadingDashboard = MutableStateFlow(true)
    val isLoadingDashboard: StateFlow<Boolean> = _isLoadingDashboard.asStateFlow()

    fun refreshDashboard() {
        viewModelScope.launch {
            _isLoadingDashboard.value = true
            delay(700)
            _isLoadingDashboard.value = false
        }
    }

    // --- Authentication State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // --- Saved Blueprints ---
    val allBlueprints: StateFlow<List<BlueprintEntity>> = repository.allBlueprints
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Active/Generated Blueprint State ---
    private val _activeBlueprint = MutableStateFlow<BlueprintEntity?>(null)
    val activeBlueprint: StateFlow<BlueprintEntity?> = _activeBlueprint.asStateFlow()

    private val _activeFloorPlanLayout = MutableStateFlow<FloorPlanLayoutData?>(null)
    val activeFloorPlanLayout: StateFlow<FloorPlanLayoutData?> = _activeFloorPlanLayout.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun updateBlueprintTagsAndCategory(blueprintId: Long, category: String, tags: String) {
        viewModelScope.launch {
            repository.updateBlueprintTagsAndCategory(blueprintId, category, tags)
            if (_activeBlueprint.value?.id == blueprintId) {
                _activeBlueprint.value = _activeBlueprint.value?.copy(category = category, tags = tags)
            }
        }
    }

    private val _costBreakdown = MutableStateFlow<CostBreakdown?>(null)
    val costBreakdown: StateFlow<CostBreakdown?> = _costBreakdown.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processStatus = MutableStateFlow("")
    val processStatus: StateFlow<String> = _processStatus.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // --- Captured Media Gallery State ---
    private val _capturedMediaList = MutableStateFlow<List<CapturedMediaItem>>(emptyList())
    val capturedMediaList: StateFlow<List<CapturedMediaItem>> = _capturedMediaList.asStateFlow()

    private val _selectedMediaIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMediaIds: StateFlow<Set<String>> = _selectedMediaIds.asStateFlow()

    fun addCapturedMedia(item: CapturedMediaItem) {
        _capturedMediaList.value = listOf(item) + _capturedMediaList.value
        _selectedMediaIds.value = _selectedMediaIds.value + item.id
    }

    fun removeCapturedMedia(id: String) {
        _capturedMediaList.value = _capturedMediaList.value.filter { it.id != id }
        _selectedMediaIds.value = _selectedMediaIds.value - id
    }

    fun clearCapturedMedia() {
        _capturedMediaList.value = emptyList()
        _selectedMediaIds.value = emptySet()
    }

    fun toggleMediaSelection(id: String) {
        val current = _selectedMediaIds.value
        _selectedMediaIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun selectAllMedia(filterType: CapturedMediaType? = null) {
        val items = if (filterType != null) {
            _capturedMediaList.value.filter { it.mediaType == filterType }
        } else {
            _capturedMediaList.value
        }
        _selectedMediaIds.value = items.map { it.id }.toSet()
    }

    fun clearMediaSelection() {
        _selectedMediaIds.value = emptySet()
    }

    fun setMediaSelection(ids: Set<String>) {
        _selectedMediaIds.value = ids
    }

    fun loadDemoSampleMedia() {
        if (_capturedMediaList.value.isEmpty()) {
            val sketchBmp = MediaThumbnailGenerator.createSampleSketchBitmap()
            val livingBmp = MediaThumbnailGenerator.createSampleSitePhotoBitmap("Living & Dining Room", 210)
            val masterBmp = MediaThumbnailGenerator.createSampleSitePhotoBitmap("Master Bedroom Suite", 260)
            val kitchenBmp = MediaThumbnailGenerator.createSampleSitePhotoBitmap("Modern Open Kitchen", 140)
            val frame1 = MediaThumbnailGenerator.createSampleSitePhotoBitmap("360 Walkthrough (Foyer)", 180)
            val frame2 = MediaThumbnailGenerator.createSampleSitePhotoBitmap("360 Walkthrough (Corridor)", 190)
            val frame3 = MediaThumbnailGenerator.createSampleSitePhotoBitmap("360 Walkthrough (Rear Porch)", 200)

            val sampleItems = listOf(
                CapturedMediaItem(
                    id = "sample_sketch_01",
                    title = "Architectural Paper Sketch (3-Bed Bungalow)",
                    mediaType = CapturedMediaType.SKETCH_IMAGE,
                    bitmap = sketchBmp,
                    estimatedRoomCount = 5,
                    notes = "Hand-drawn pencil sketch with dimension annotations in meters.",
                    qualityScore = 96,
                    resolutionLabel = "1920×1080 (High Contrast)"
                ),
                CapturedMediaItem(
                    id = "sample_photo_multi_01",
                    title = "Multi-Angle Room Perspectives (3 Angles)",
                    mediaType = CapturedMediaType.MULTI_ANGLE_PHOTOS,
                    bitmap = livingBmp,
                    secondaryBitmaps = listOf(masterBmp, kitchenBmp),
                    estimatedRoomCount = 4,
                    notes = "Captured from room corner looking towards exterior sliding door.",
                    qualityScore = 93,
                    resolutionLabel = "2048×1536 (3 Angles)"
                ),
                CapturedMediaItem(
                    id = "sample_video_walkthrough_01",
                    title = "360° Interior Walkthrough Scan",
                    mediaType = CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES,
                    bitmap = frame1,
                    secondaryBitmaps = listOf(frame2, frame3),
                    videoDurationSeconds = 18,
                    estimatedRoomCount = 6,
                    notes = "Continuous clockwise pacing around internal perimeter.",
                    qualityScore = 98,
                    resolutionLabel = "1080p HD (60fps)",
                    telemetry = WalkthroughTelemetry(
                        durationSeconds = 18,
                        estimatedPacedDistanceMeters = 16.5f,
                        detectedRoomsCount = 5
                    )
                ),
                CapturedMediaItem(
                    id = "sample_photo_single_01",
                    title = "Master Suite & Ensuite Site Photo",
                    mediaType = CapturedMediaType.PHYSICAL_PHOTO,
                    bitmap = masterBmp,
                    estimatedRoomCount = 2,
                    notes = "Exterior wall orientation facing North-East.",
                    qualityScore = 91,
                    resolutionLabel = "1920×1080 FHD"
                )
            )

            _capturedMediaList.value = sampleItems
            // Select all by default for immediate convenience
            _selectedMediaIds.value = sampleItems.map { it.id }.toSet()
        }
    }

    init {
        // Initial simulated fetch to provide smooth skeleton shimmer loading experience
        viewModelScope.launch {
            delay(600)
            _isLoadingDashboard.value = false
        }

        // Initialize sample demo media items for gallery
        loadDemoSampleMedia()

        // Create an initial default walkthrough/demo blueprint if list is empty
        viewModelScope.launch {
            allBlueprints.collect { list ->
                if (list.isEmpty() && _activeBlueprint.value == null) {
                    createDemoBlueprint()
                }
            }
        }
    }

    // --- Authenticators ---
    fun loginUser(email: String, pin: String) {
        viewModelScope.launch {
            _loginError.value = null
            val user = repository.getUserByEmail(email)
            if (user != null) {
                // simple hash simulation
                val inputHash = pin.hashCode().toString()
                if (user.passcodeHash == inputHash) {
                    _currentUser.value = user
                } else {
                    _loginError.value = "Incorrect security PIN."
                }
            } else {
                _loginError.value = "User not found. Register a new account."
            }
        }
    }

    fun registerUser(name: String, email: String, pin: String) {
        viewModelScope.launch {
            _loginError.value = null
            if (name.isBlank() || email.isBlank() || pin.isBlank()) {
                _loginError.value = "All fields are required."
                return@launch
            }
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                _loginError.value = "Email is already registered."
                return@launch
            }
            val hash = pin.hashCode().toString()
            val newUser = UserEntity(email, name, hash)
            repository.registerUser(newUser)
            _currentUser.value = newUser
        }
    }

    fun loginWithSocialProvider(
        providerName: String,
        email: String,
        name: String,
        role: String = "Licensed Architect & Builder",
        county: String = "Nairobi",
        avatarColor: String = "#00E5FF"
    ) {
        viewModelScope.launch {
            _loginError.value = null
            var user = repository.getUserByEmail(email)
            if (user == null) {
                user = UserEntity(
                    email = email,
                    name = name,
                    passcodeHash = "social_sso_$providerName".hashCode().toString(),
                    biometricEnabled = true,
                    role = role,
                    county = county,
                    avatarColorHex = avatarColor
                )
                repository.registerUser(user)
            }
            _currentUser.value = user
        }
    }

    fun loginWithBiometrics(defaultEmail: String = "homeowner@nyumbaplan.ke") {
        viewModelScope.launch {
            _loginError.value = null
            var user = repository.getUserByEmail(defaultEmail)
            if (user == null) {
                user = UserEntity(
                    email = defaultEmail,
                    name = "Verified Biometric Homeowner",
                    passcodeHash = "2540".hashCode().toString(),
                    biometricEnabled = true,
                    role = "Homeowner & Builder",
                    county = "Nairobi",
                    avatarColorHex = "#00E5FF"
                )
                repository.registerUser(user)
            }
            _currentUser.value = user
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeBlueprint.value = null
        _activeFloorPlanLayout.value = null
        _costBreakdown.value = null
        _selectedCategoryFilter.value = "All"
        _loginError.value = null
    }

    fun updateUserProfile(name: String, role: String, phone: String, county: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(
                name = name.ifBlank { user.name },
                role = role.ifBlank { user.role },
                phone = phone.ifBlank { user.phone },
                county = county.ifBlank { user.county }
            )
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    fun updateUserPreferences(currency: String, unit: String, preset: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(
                preferredCurrency = currency,
                preferredUnit = unit,
                constructionPreset = preset
            )
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(biometricEnabled = enabled)
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    fun updateAvatarColor(colorHex: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(avatarColorHex = colorHex)
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    fun updateSecurityPin(currentPin: String, newPin: String, onResult: (Boolean, String) -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            onResult(false, "User not logged in.")
            return
        }
        val currentHash = currentPin.hashCode().toString()
        if (user.passcodeHash != currentHash) {
            onResult(false, "Current PIN is incorrect.")
            return
        }
        if (newPin.length < 4) {
            onResult(false, "New PIN must be at least 4 digits.")
            return
        }
        val newHash = newPin.hashCode().toString()
        viewModelScope.launch {
            val updated = user.copy(passcodeHash = newHash)
            repository.updateUser(updated)
            _currentUser.value = updated
            onResult(true, "Security PIN updated successfully!")
        }
    }

    // --- Core Floorplan Operations ---
    fun generateLocalRuleBasedPlan(
        numBedrooms: Int,
        numBathrooms: Int,
        finishesQuality: String,
        roofType: String,
        soilType: String,
        targetAreaSqm: Float,
        title: String,
        county: String = "Nairobi"
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processStatus.value = "Aligning room layout boundaries..."
            
            // Local algorithmic grid allocation
            val rooms = BlueprintEngine.generateFloorPlan(numBedrooms, numBathrooms, finishesQuality, targetAreaSqm)
            
            _processStatus.value = "Calculating itemized NCA construction costs..."
            val breakdown = BlueprintEngine.calculateCostBreakdown(targetAreaSqm, finishesQuality, roofType, soilType)
            _costBreakdown.value = breakdown

            // Serialize RoomItems to JSON Array
            val roomsArray = JSONArray()
            rooms.forEach { r ->
                val o = org.json.JSONObject()
                o.put("name", r.name)
                o.put("width", r.width.toDouble())
                o.put("height", r.height.toDouble())
                o.put("x", r.x.toDouble())
                o.put("y", r.y.toDouble())
                o.put("color", r.color)
                roomsArray.put(o)
            }

            val entity = BlueprintEntity(
                title = title.ifBlank { "Custom $numBedrooms-Bed Floor Plan" },
                description = "Algorithmic rule-based residential blueprint for a single-storey building with $numBedrooms bedrooms & $numBathrooms bathrooms under $roofType roofing on $soilType soil.",
                roomsJson = roomsArray.toString(),
                totalAreaSqm = targetAreaSqm,
                estimatedCostMinKes = breakdown.totalCostMin,
                estimatedCostMaxKes = breakdown.totalCostMax,
                plotSize = "50x100 ft",
                county = county,
                notes = "Rule-based generation conforming to Kenyan Physical Planning Act standards. Estimations are preliminary."
            )

            val newId = repository.insertBlueprint(entity)
            _activeBlueprint.value = entity.copy(id = newId)
            _isProcessing.value = false
        }
    }

    fun applyBuildingTemplate(
        template: BuildingTemplate,
        customTitle: String,
        customAreaSqm: Float,
        county: String,
        finishesQuality: String,
        roofType: String,
        includedModules: List<String>,
        storeys: Int = template.defaultStoreys,
        onCreated: (BlueprintEntity) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val effectiveStoreys = storeys.coerceIn(template.minStoreys, template.maxStoreys)
            _processStatus.value = if (effectiveStoreys > 1) {
                "Engineering $effectiveStoreys-storey structural layout from '${template.title}'..."
            } else {
                "Generating layout from '${template.title}'..."
            }

            val entity = BuildingTemplateCatalog.createBlueprintFromCustomization(
                template = template,
                customTitle = customTitle,
                customAreaSqm = customAreaSqm,
                county = county,
                finishesQuality = finishesQuality,
                roofType = roofType,
                includedModules = includedModules,
                storeys = effectiveStoreys
            )

            val newId = repository.insertBlueprint(entity)
            val finalBlueprint = entity.copy(id = newId)
            _activeBlueprint.value = finalBlueprint
            _costBreakdown.value = BlueprintEngine.calculateCostBreakdown(
                areaSqm = finalBlueprint.totalAreaSqm,
                finishesQuality = finishesQuality,
                roofType = roofType
            )
            _isProcessing.value = false
            onCreated(finalBlueprint)
        }
    }

    /**
     * Analyzes single captured media (sketch image or photo) via Gemini Floor Plan Analysis Service.
     */
    fun analyzeCapturedMedia(
        bitmap: Bitmap,
        mediaType: CapturedMediaType = CapturedMediaType.SKETCH_IMAGE,
        extraNotes: String = "",
        county: String = "Nairobi",
        specs: BlueprintGenerationSpecs? = null,
        onComplete: ((BlueprintEntity) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _analysisError.value = null
            _processStatus.value = "Analyzing ${mediaType.label} with Gemini Vision AI..."

            val result = floorPlanAnalysisService.analyzeFloorPlanImage(
                bitmap = bitmap,
                mediaType = mediaType,
                userPrompt = extraNotes,
                contextCounty = county,
                specs = specs
            )

            handleAnalysisResult(result, county, onComplete)
        }
    }

    /**
     * Analyzes multiple captured photos from different angles via Gemini Floor Plan Analysis Service.
     */
    fun analyzeMultiAnglePhotos(
        bitmaps: List<Bitmap>,
        extraNotes: String = "",
        county: String = "Nairobi",
        specs: BlueprintGenerationSpecs? = null,
        onComplete: ((BlueprintEntity) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _analysisError.value = null
            _processStatus.value = "Synthesizing ${bitmaps.size} perspective angles with Gemini Vision..."

            val result = floorPlanAnalysisService.analyzeMultiAnglePhotos(
                bitmaps = bitmaps,
                userPrompt = extraNotes,
                contextCounty = county,
                specs = specs
            )

            handleAnalysisResult(result, county, onComplete)
        }
    }

    /**
     * Analyzes video frames captured from a room walkthrough scan with telemetry telemetry.
     */
    fun analyzeWalkthroughVideoFrames(
        frames: List<Bitmap>,
        telemetry: WalkthroughTelemetry? = null,
        county: String = "Nairobi",
        extraNotes: String = "",
        specs: BlueprintGenerationSpecs? = null,
        onComplete: ((BlueprintEntity) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _analysisError.value = null
            _processStatus.value = "Analyzing walkthrough video telemetry & keyframes with Gemini..."

            val result = floorPlanAnalysisService.analyzeWalkthroughVideoFrames(
                frames = frames,
                telemetry = telemetry,
                userPrompt = extraNotes,
                contextCounty = county,
                specs = specs
            )

            handleAnalysisResult(result, county, onComplete)
        }
    }

    private suspend fun handleAnalysisResult(
        result: Result<FloorPlanLayoutData>,
        county: String,
        onComplete: ((BlueprintEntity) -> Unit)?
    ) {
        if (result.isSuccess) {
            val layout = result.getOrThrow()
            _activeFloorPlanLayout.value = layout
            _processStatus.value = "Constructing 2D CAD blueprint & NCA cost schedule..."

            val breakdown = layout.toCostBreakdown()
            _costBreakdown.value = breakdown

            val entity = layout.toBlueprintEntity(county = county)
            val insertedId = repository.insertBlueprint(entity)
            val finalBlueprint = entity.copy(id = insertedId)
            _activeBlueprint.value = finalBlueprint
            _isProcessing.value = false
            onComplete?.invoke(finalBlueprint)
        } else {
            val err = result.exceptionOrNull()?.localizedMessage ?: "Analysis failed."
            _analysisError.value = err
            _isProcessing.value = false
        }
    }

    fun analyzeSketchWithGemini(
        bitmap: Bitmap,
        extraNotes: String,
        county: String = "Nairobi",
        onComplete: ((BlueprintEntity) -> Unit)? = null
    ) {
        analyzeCapturedMedia(
            bitmap = bitmap,
            mediaType = CapturedMediaType.SKETCH_IMAGE,
            extraNotes = extraNotes,
            county = county,
            onComplete = onComplete
        )
    }

    fun loadBlueprint(blueprint: BlueprintEntity) {
        _activeBlueprint.value = blueprint
        // Re-calculate breakdown
        _costBreakdown.value = BlueprintEngine.calculateCostBreakdown(
            areaSqm = blueprint.totalAreaSqm,
            finishesQuality = "Standard"
        )
    }

    fun saveCurrentProgress(
        title: String? = null,
        description: String? = null,
        county: String? = null,
        plotSize: String? = null,
        notes: String? = null,
        status: String? = null,
        category: String? = null,
        tags: String? = null,
        updatedRooms: List<RoomItem>? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val current = _activeBlueprint.value ?: return@launch
            
            val roomsToSave = updatedRooms ?: try {
                val list = mutableListOf<RoomItem>()
                val arr = JSONArray(current.roomsJson)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        RoomItem(
                            name = o.getString("name"),
                            width = o.getDouble("width").toFloat(),
                            height = o.getDouble("height").toFloat(),
                            x = o.getDouble("x").toFloat(),
                            y = o.getDouble("y").toFloat(),
                            color = o.optString("color", "#2196F3")
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }

            val totalArea = if (roomsToSave.isNotEmpty()) {
                roomsToSave.sumOf { (it.width * it.height).toDouble() }.toFloat()
            } else current.totalAreaSqm

            val breakdown = BlueprintEngine.calculateCostBreakdown(totalArea, "Standard")

            val roomsArray = JSONArray()
            roomsToSave.forEach { r ->
                val o = org.json.JSONObject()
                o.put("name", r.name)
                o.put("width", r.width.toDouble())
                o.put("height", r.height.toDouble())
                o.put("x", r.x.toDouble())
                o.put("y", r.y.toDouble())
                o.put("color", r.color)
                roomsArray.put(o)
            }

            val updatedEntity = current.copy(
                title = title?.ifBlank { current.title } ?: current.title,
                description = description ?: current.description,
                county = county ?: current.county,
                plotSize = plotSize ?: current.plotSize,
                notes = notes ?: current.notes,
                status = status ?: current.status,
                category = category ?: current.category,
                tags = tags ?: current.tags,
                roomsJson = if (roomsToSave.isNotEmpty()) roomsArray.toString() else current.roomsJson,
                totalAreaSqm = totalArea,
                estimatedCostMinKes = breakdown.totalCostMin,
                estimatedCostMaxKes = breakdown.totalCostMax,
                createdAt = System.currentTimeMillis()
            )

            val savedId = repository.insertBlueprint(updatedEntity)
            val finalEntity = updatedEntity.copy(id = savedId)
            _activeBlueprint.value = finalEntity
            _costBreakdown.value = breakdown
            onComplete?.invoke(true)
        }
    }

    fun updateBlueprintRooms(blueprintId: Long, updatedRooms: List<RoomItem>) {
        viewModelScope.launch {
            val blueprint = repository.getBlueprintById(blueprintId) ?: return@launch
            
            // Re-calculate total area based on rooms
            val totalArea = updatedRooms.sumOf { (it.width * it.height).toDouble() }.toFloat()
            
            // Re-calculate costings
            val breakdown = BlueprintEngine.calculateCostBreakdown(
                areaSqm = totalArea,
                finishesQuality = "Standard"
            )
            
            // Serialize updated rooms list
            val roomsArray = JSONArray()
            updatedRooms.forEach { r ->
                val o = org.json.JSONObject()
                o.put("name", r.name)
                o.put("width", r.width.toDouble())
                o.put("height", r.height.toDouble())
                o.put("x", r.x.toDouble())
                o.put("y", r.y.toDouble())
                o.put("color", r.color)
                roomsArray.put(o)
            }
            
            val updatedEntity = blueprint.copy(
                roomsJson = roomsArray.toString(),
                totalAreaSqm = totalArea,
                estimatedCostMinKes = breakdown.totalCostMin,
                estimatedCostMaxKes = breakdown.totalCostMax,
                createdAt = System.currentTimeMillis()
            )
            
            repository.insertBlueprint(updatedEntity)
            
            // If active, reload
            if (_activeBlueprint.value?.id == blueprintId) {
                _activeBlueprint.value = updatedEntity
                _costBreakdown.value = breakdown
            }
        }
    }

    fun deleteBlueprint(id: Long) {
        viewModelScope.launch {
            repository.deleteBlueprintById(id)
            if (_activeBlueprint.value?.id == id) {
                _activeBlueprint.value = null
                _costBreakdown.value = null
            }
        }
    }

    fun duplicateBlueprint(id: Long) {
        viewModelScope.launch {
            val blueprint = repository.getBlueprintById(id) ?: return@launch
            val duplicated = blueprint.copy(
                id = 0L,
                title = "${blueprint.title} (Copy)",
                createdAt = System.currentTimeMillis()
            )
            repository.insertBlueprint(duplicated)
        }
    }

    /**
     * Analyzes all user-selected media items from the thumbnail gallery with Gemini Vision AI.
     */
    fun analyzeSelectedMediaItems(
        extraNotes: String = "",
        county: String = "Nairobi",
        specs: BlueprintGenerationSpecs? = null,
        onComplete: ((BlueprintEntity) -> Unit)? = null
    ) {
        val selectedItems = _capturedMediaList.value.filter { it.id in _selectedMediaIds.value }
        if (selectedItems.isEmpty()) {
            _analysisError.value = "Please select at least one captured photo, sketch, or video walkthrough."
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _analysisError.value = null

            val allBitmaps = selectedItems.flatMap { it.allBitmaps }
            val hasVideo = selectedItems.any { it.mediaType == CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES }
            val videoItem = selectedItems.firstOrNull { it.mediaType == CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES }

            val combinedNotes = buildString {
                if (extraNotes.isNotBlank()) {
                    append(extraNotes).append("\n\n")
                }
                append("Selected Media Breakdown (${selectedItems.size} items):\n")
                selectedItems.forEachIndexed { i, item ->
                    append("- ${item.title} (${item.mediaType.label}, ${item.totalFramesCount} frame(s))\n")
                    if (item.notes.isNotBlank()) {
                        append("  Notes: ${item.notes}\n")
                    }
                }
            }

            if (hasVideo && videoItem != null) {
                _processStatus.value = "Analyzing walkthrough video keyframes and perspective angles with Gemini Vision..."
                val telemetry = videoItem.telemetry ?: WalkthroughTelemetry(
                    durationSeconds = videoItem.videoDurationSeconds.coerceAtLeast(10),
                    estimatedPacedDistanceMeters = (videoItem.videoDurationSeconds * 1.0f).coerceAtLeast(10f),
                    detectedRoomsCount = 4
                )
                val result = floorPlanAnalysisService.analyzeWalkthroughVideoFrames(
                    frames = allBitmaps,
                    telemetry = telemetry,
                    contextCounty = county,
                    userPrompt = combinedNotes,
                    specs = specs
                )
                handleAnalysisResult(result, county, onComplete)
            } else if (allBitmaps.size > 1) {
                _processStatus.value = "Synthesizing ${allBitmaps.size} perspective angles & sketches with Gemini Vision..."
                val result = floorPlanAnalysisService.analyzeMultiAnglePhotos(
                    bitmaps = allBitmaps,
                    userPrompt = combinedNotes,
                    contextCounty = county,
                    specs = specs
                )
                handleAnalysisResult(result, county, onComplete)
            } else if (allBitmaps.isNotEmpty()) {
                val singleItem = selectedItems.first()
                _processStatus.value = "Analyzing ${singleItem.mediaType.label} with Gemini Vision AI..."
                val result = floorPlanAnalysisService.analyzeFloorPlanImage(
                    bitmap = allBitmaps.first(),
                    mediaType = singleItem.mediaType,
                    userPrompt = combinedNotes,
                    contextCounty = county,
                    specs = specs
                )
                handleAnalysisResult(result, county, onComplete)
            } else {
                _analysisError.value = "No image frames found in selected media items."
                _isProcessing.value = false
            }
        }
    }

    private suspend fun createDemoBlueprint() {
        // Ensure a simple initial demo exists for first run
        val demoRooms = listOf(
            RoomItem("Living Room", 5f, 4.5f, 0f, 0f, "#2196F3"),
            RoomItem("Kitchen", 3.5f, 4.5f, 5f, 0f, "#4CAF50"),
            RoomItem("Master Bed", 4f, 4f, 0f, 4.5f, "#9C27B0"),
            RoomItem("Bathroom", 2.5f, 2.5f, 4f, 4.5f, "#FF9800"),
            RoomItem("Passage", 1.5f, 4f, 6.5f, 4.5f, "#9E9E9E")
        )

        val roomsArray = JSONArray()
        demoRooms.forEach { r ->
            val o = org.json.JSONObject()
            o.put("name", r.name)
            o.put("width", r.width.toDouble())
            o.put("height", r.height.toDouble())
            o.put("x", r.x.toDouble())
            o.put("y", r.y.toDouble())
            o.put("color", r.color)
            roomsArray.put(o)
        }

        val breakdown = BlueprintEngine.calculateCostBreakdown(58f, "Standard")

        val entity = BlueprintEntity(
            title = "Demo 1-Bedroom Layout",
            description = "A standard compact single-storey resident design layout optimized for standard 50x100 plots in Kenya.",
            roomsJson = roomsArray.toString(),
            totalAreaSqm = 58f,
            estimatedCostMinKes = breakdown.totalCostMin,
            estimatedCostMaxKes = breakdown.totalCostMax,
            plotSize = "50x100 ft",
            status = "Approved",
            notes = "Standard model setup for introductory visualization."
        )

        repository.insertBlueprint(entity)
    }
}

class ViewModelFactory(
    private val repository: BlueprintRepository,
    private val floorPlanAnalysisService: GeminiFloorPlanAnalysisService = GeminiApiClient.service
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlueprintViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BlueprintViewModel(repository, floorPlanAnalysisService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

object MediaThumbnailGenerator {
    fun createSampleSketchBitmap(): Bitmap {
        val width = 480
        val height = 360
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Blueprint dark grid background
        paint.color = android.graphics.Color.rgb(15, 32, 67)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Grid lines
        paint.color = android.graphics.Color.argb(45, 0, 229, 255)
        paint.strokeWidth = 1f
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
            x += 24f
        }
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
            y += 24f
        }

        // Outer architectural wall
        paint.color = android.graphics.Color.rgb(0, 229, 255)
        paint.strokeWidth = 4f
        paint.style = android.graphics.Paint.Style.STROKE
        canvas.drawRect(36f, 36f, width - 36f, height - 36f, paint)

        // Room partitions
        canvas.drawLine(240f, 36f, 240f, height - 36f, paint)
        canvas.drawLine(36f, 200f, 240f, 200f, paint)
        canvas.drawLine(240f, 180f, width - 36f, 180f, paint)

        // Room Labels
        paint.style = android.graphics.Paint.Style.FILL
        paint.textSize = 13f
        paint.color = android.graphics.Color.WHITE
        canvas.drawText("LIVING ROOM (5.2m x 4.0m)", 48f, 110f, paint)
        canvas.drawText("KITCHEN (3.8m x 3.2m)", 48f, 260f, paint)
        canvas.drawText("MASTER BED (4.2m x 3.6m)", 252f, 100f, paint)
        canvas.drawText("BEDROOM 2 (3.5m x 3.2m)", 252f, 260f, paint)

        return bmp
    }

    fun createSampleSitePhotoBitmap(roomName: String, hueSeed: Int = 200): Bitmap {
        val width = 480
        val height = 360
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Room backdrop
        paint.color = android.graphics.Color.rgb(30, 41, 59)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Perspective wall lines
        paint.color = android.graphics.Color.rgb(51, 65, 85)
        paint.style = android.graphics.Paint.Style.FILL
        val path = android.graphics.Path()
        path.moveTo(60f, 60f)
        path.lineTo(width - 60f, 60f)
        path.lineTo(width - 60f, height - 60f)
        path.lineTo(60f, height - 60f)
        path.close()
        canvas.drawPath(path, paint)

        // Wall accents
        paint.color = android.graphics.Color.argb(120, 0, 229, 255)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawLine(0f, 0f, 60f, 60f, paint)
        canvas.drawLine(width.toFloat(), 0f, width - 60f, 60f, paint)
        canvas.drawLine(0f, height.toFloat(), 60f, height - 60f, paint)
        canvas.drawLine(width.toFloat(), height.toFloat(), width - 60f, height - 60f, paint)

        // Room title badge
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.argb(200, 15, 23, 42)
        canvas.drawRoundRect(60f, 150f, width - 60f, 210f, 12f, 12f, paint)

        paint.color = android.graphics.Color.rgb(0, 229, 255)
        paint.textSize = 14f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("SITE PERSPECTIVE: $roomName", width / 2f, 185f, paint)

        return bmp
    }
}


