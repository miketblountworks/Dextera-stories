package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.CharacterEntity
import com.example.data.database.StoryDatabase
import com.example.data.database.StoryPageEntity
import com.example.data.database.StoryStateEntity
import com.example.data.repository.StoryRepository
import com.example.service.lighting.PhilipsHueLightManager
import com.example.service.lighting.SmartLightManager
import com.example.service.llm.LiteRTLMStoryGenerator
import com.example.service.tts.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoryUiState(
    val protagonistName: String = "",
    val protagonistAge: Int = 6,
    val supportingCharacters: List<CharacterEntity> = emptyList(),
    
    // Core Story status
    val isStoryActive: Boolean = false,
    val currentPageNumber: Int = 1,
    val pages: List<StoryPageEntity> = emptyList(),
    val currentStoryText: String = "",
    val currentChoices: List<String> = emptyList(),
    val currentLightingEffect: String = "WHITE_LIGHT",
    
    // State indicators
    val isGenerating: Boolean = false,
    val isSpeaking: Boolean = false,
    val hasCameraPermission: Boolean = false,
    
    // Philips Hue Settings
    val hueBridgeIp: String = "192.168.1.100",
    val hueUsername: String = "newdeveloper",
    val hueLightId: String = "1",
    val isHueConfigured: Boolean = false
)

class StoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StoryRepository
    private val generator: LiteRTLMStoryGenerator
    private val ttsManager: TtsManager
    private val lightManager: SmartLightManager

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    init {
        val database = StoryDatabase.getDatabase(application)
        repository = StoryRepository(database.characterDao(), database.storyDao())
        generator = LiteRTLMStoryGenerator(application)
        ttsManager = TtsManager(application)
        lightManager = PhilipsHueLightManager()

        // Sync repository data to our ViewModel state
        viewModelScope.launch {
            repository.allCharacters.collect { characters ->
                _uiState.update { it.copy(supportingCharacters = characters) }
            }
        }

        viewModelScope.launch {
            repository.storyState.collect { state ->
                if (state != null) {
                    _uiState.update {
                        it.copy(
                            protagonistName = state.protagonistName,
                            protagonistAge = state.protagonistAge,
                            currentPageNumber = state.currentPageNumber,
                            isStoryActive = true
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.allPages.collect { pages ->
                _uiState.update { it.copy(pages = pages) }
                updateCurrentPageData()
            }
        }
    }

    fun updateProtagonist(name: String, age: Int) {
        _uiState.update { it.copy(protagonistName = name, protagonistAge = age) }
    }

    fun addSupportingCharacter(name: String, gender: String, alignment: String, traits: String) {
        viewModelScope.launch {
            repository.insertCharacter(
                CharacterEntity(
                    name = name,
                    gender = gender,
                    alignment = alignment,
                    personalityTraits = traits
                )
            )
        }
    }

    fun deleteSupportingCharacter(character: CharacterEntity) {
        viewModelScope.launch {
            repository.deleteCharacter(character)
        }
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun configureHue(bridgeIp: String, username: String, lightId: String) {
        _uiState.update {
            it.copy(
                hueBridgeIp = bridgeIp,
                hueUsername = username,
                hueLightId = lightId,
                isHueConfigured = bridgeIp.isNotEmpty() && username.isNotEmpty() && lightId.isNotEmpty()
            )
        }
        lightManager.configure(bridgeIp, username, lightId)
    }

    fun startNewStory() {
        if (_uiState.value.protagonistName.isEmpty()) return
        
        viewModelScope.launch {
            repository.resetStory()
            val initialState = StoryStateEntity(
                protagonistName = _uiState.value.protagonistName,
                protagonistAge = _uiState.value.protagonistAge,
                currentPageNumber = 1
            )
            repository.insertStoryState(initialState)
            
            // Generate the very first page
            generateNextPage(null)
        }
    }

    fun selectChoice(choice: String) {
        val nextPage = _uiState.value.currentPageNumber + 1
        viewModelScope.launch {
            repository.updateCurrentPage(nextPage)
            generateNextPage(choice)
        }
    }

    private fun generateNextPage(choice: String?) {
        _uiState.update { it.copy(isGenerating = true) }
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // Build a quick text summary of the previous pages to feed into LLM memory
            val historySummary = currentState.pages.joinToString("\n") {
                "Page ${it.pageNumber}: ${it.storyText}"
            }

            // Call our dual-mode Story Generator (LiteRT-LM with high-fidelity fallback)
            val generated = generator.generatePage(
                protagonistName = currentState.protagonistName,
                protagonistAge = currentState.protagonistAge,
                supportingCharacters = currentState.supportingCharacters,
                pageNumber = currentState.currentPageNumber,
                chosenChoice = choice,
                historySummary = historySummary
            )

            val pageEntity = StoryPageEntity(
                pageNumber = currentState.currentPageNumber,
                storyText = generated.storyText,
                choice1 = generated.choices.getOrNull(0) ?: "",
                choice2 = generated.choices.getOrNull(1) ?: "",
                lightingEffect = generated.lightingEffect
            )

            repository.insertPage(pageEntity)
            _uiState.update { it.copy(isGenerating = false) }
            
            // Automatically play TTS and Light sync for the new page
            syncEffectsAndSpeech(pageEntity)
        }
    }

    private fun updateCurrentPageData() {
        val state = _uiState.value
        val currentPage = state.pages.find { it.pageNumber == state.currentPageNumber }
        if (currentPage != null) {
            _uiState.update {
                it.copy(
                    currentStoryText = currentPage.storyText,
                    currentChoices = listOfNotNull(
                        currentPage.choice1.takeIf { c -> c.isNotEmpty() },
                        currentPage.choice2.takeIf { c -> c.isNotEmpty() }
                    ),
                    currentLightingEffect = currentPage.lightingEffect
                )
            }
        }
    }

    fun replayPage() {
        val state = _uiState.value
        val currentPage = state.pages.find { it.pageNumber == state.currentPageNumber }
        if (currentPage != null) {
            syncEffectsAndSpeech(currentPage)
        }
    }

    private fun syncEffectsAndSpeech(page: StoryPageEntity) {
        viewModelScope.launch {
            // 1. Trigger lighting effect simultaneously with Text-to-Speech
            launch {
                try {
                    lightManager.triggerEffect(page.lightingEffect)
                } catch (e: Exception) {
                    Log.e("StoryViewModel", "Failed to trigger smart light effect: ${e.message}")
                }
            }

            // 2. Play native Android Text-To-Speech engine
            ttsManager.speak(
                text = page.storyText,
                onStart = {
                    _uiState.update { it.copy(isSpeaking = true) }
                },
                onDone = {
                    _uiState.update { it.copy(isSpeaking = false) }
                }
            )
        }
    }

    fun navigateBack() {
        val prevPage = _uiState.value.currentPageNumber - 1
        if (prevPage >= 1) {
            viewModelScope.launch {
                repository.updateCurrentPage(prevPage)
            }
        }
    }

    fun navigateForward() {
        val nextPage = _uiState.value.currentPageNumber + 1
        val maxPageGenerated = _uiState.value.pages.maxOfOrNull { it.pageNumber } ?: 1
        if (nextPage <= maxPageGenerated) {
            viewModelScope.launch {
                repository.updateCurrentPage(nextPage)
            }
        }
    }

    fun resetStorySession() {
        viewModelScope.launch {
            repository.resetStory()
            _uiState.update {
                it.copy(
                    isStoryActive = false,
                    currentPageNumber = 1,
                    pages = emptyList(),
                    currentStoryText = "",
                    currentChoices = emptyList()
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
