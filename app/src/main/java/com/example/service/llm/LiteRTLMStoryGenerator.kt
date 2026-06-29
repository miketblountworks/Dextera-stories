package com.example.service.llm

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.database.CharacterEntity
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class LiteRTLMStoryGenerator(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var isLocalLlmInitialized = false
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Initializes the local on-device LiteRT LLM using a model file (.bin).
     * Since the user will need to supply the quantized model (like Gemma-2B),
     * we look for a path on-device or log instructions if not yet available.
     */
    fun initializeLocalLlm(modelPath: String): Boolean {
        return try {
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                Log.w("LiteRTLMStoryGenerator", "Local model file not found at: $modelPath. Running in Fallback Mode.")
                isLocalLlmInitialized = false
                return false
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(512)
                .setTemperature(0.7f)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isLocalLlmInitialized = true
            Log.d("LiteRTLMStoryGenerator", "On-device Gemma LLM initialized successfully.")
            true
        } catch (e: Exception) {
            Log.e("LiteRTLMStoryGenerator", "Failed to initialize local LLM: ${e.message}", e)
            isLocalLlmInitialized = false
            false
        }
    }

    /**
     * Generates a story page based on the current state.
     */
    suspend fun generatePage(
        protagonistName: String,
        protagonistAge: Int,
        supportingCharacters: List<CharacterEntity>,
        pageNumber: Int,
        chosenChoice: String?,
        historySummary: String
    ): StoryPageResponse = withContext(Dispatchers.Default) {
        val prompt = buildStoryPrompt(
            protagonistName,
            protagonistAge,
            supportingCharacters,
            pageNumber,
            chosenChoice,
            historySummary
        )

        Log.d("LiteRTLMStoryGenerator", "Generating Page $pageNumber using Prompt:\n$prompt")

        if (isLocalLlmInitialized && llmInference != null) {
            try {
                val rawResponse = llmInference!!.generateResponse(prompt)
                Log.d("LiteRTLMStoryGenerator", "Local LLM Raw Response:\n$rawResponse")
                parseStoryResponse(rawResponse, pageNumber)
            } catch (e: Exception) {
                Log.e("LiteRTLMStoryGenerator", "Local LLM generation failed, falling back...", e)
                generateWithCloudOrMock(prompt, pageNumber)
            }
        } else {
            generateWithCloudOrMock(prompt, pageNumber)
        }
    }

    private suspend fun generateWithCloudOrMock(prompt: String, pageNumber: Int): StoryPageResponse {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                return callGeminiApi(prompt, apiKey, pageNumber)
            } catch (e: Exception) {
                Log.e("LiteRTLMStoryGenerator", "Gemini API call failed, falling back to local story engine", e)
            }
        }
        return generateRuleBasedStory(pageNumber)
    }

    private suspend fun callGeminiApi(prompt: String, apiKey: String, pageNumber: Int): StoryPageResponse = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        
        // Instruct Gemini to strictly return valid JSON
        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$prompt\n\nIMPORTANT: Respond with standard raw parseable JSON only. Do not wrap in ```json or markdown blocks.")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }

        val requestBody = requestBodyJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP Error: ${response.code} - ${response.message}")
            }
            val responseString = response.body?.string() ?: throw Exception("Empty response body")
            Log.d("LiteRTLMStoryGenerator", "Gemini Cloud Response:\n$responseString")
            
            val jsonResponse = JSONObject(responseString)
            val text = jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            parseStoryResponse(text, pageNumber)
        }
    }

    private fun parseStoryResponse(rawText: String, pageNumber: Int): StoryPageResponse {
        return try {
            // Remove markdown code blocks if the LLM output contains any
            var sanitized = rawText.trim()
            if (sanitized.startsWith("```json")) {
                sanitized = sanitized.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (sanitized.startsWith("```")) {
                sanitized = sanitized.substringAfter("```").substringBeforeLast("```").trim()
            }

            val json = JSONObject(sanitized)
            val storyText = json.getString("story_text")
            
            val choicesArray = json.optJSONArray("choices")
            val choices = mutableListOf<String>()
            if (choicesArray != null) {
                for (i in 0 until choicesArray.length()) {
                    choices.add(choicesArray.getString(i))
                }
            }

            val effect = json.optString("lighting_effect", "WHITE_LIGHT").uppercase()

            StoryPageResponse(
                storyText = storyText,
                choices = choices,
                lightingEffect = effect
            )
        } catch (e: Exception) {
            Log.e("LiteRTLMStoryGenerator", "Error parsing LLM response JSON. Raw: $rawText", e)
            // Generate clean default page
            val isClimax = pageNumber >= 20
            StoryPageResponse(
                storyText = "As you venture forward, the mysterious energy surrounds you. The path splits before you once more, whispering ancient secrets.",
                choices = if (isClimax) listOf("Accept your fate", "Reach for the stars") else listOf("Explore the glowing cavern", "Climb the grand staircase"),
                lightingEffect = if (isClimax) "SPOOKY_PURPLE" else "CALM_BLUE"
            )
        }
    }

    /**
     * Constructs the comprehensive prompt guiding the LLM
     */
    private fun buildStoryPrompt(
        protagonistName: String,
        protagonistAge: Int,
        supportingCharacters: List<CharacterEntity>,
        pageNumber: Int,
        chosenChoice: String?,
        historySummary: String
    ): String {
        val characterListStr = if (supportingCharacters.isEmpty()) {
            "No supporting characters yet."
        } else {
            supportingCharacters.joinToString("\n") { 
                "- ${it.name} (Gender: ${it.gender}, Alignment: ${it.alignment}, Traits: ${it.personalityTraits})"
            }
        }

        val actionType = if (pageNumber >= 20) {
            "STEER TOWARDS CLIMAX & CONCLUSION: The story is now on Page $pageNumber (Climax phase). Build massive tension, resolve the narrative arc based on past actions, and offer concluding choices. If the narrative resolves, choices can be empty or indicate ending paths."
        } else {
            "RISING ACTION (Page $pageNumber of 20): Deepen the rising action, escalate complications, introduce challenges, and always provide exactly 2 meaningful choices to branch the narrative."
        }

        return """
            You are an expert children's fantasy storyteller. Write Page $pageNumber of an interactive branching storybook.
            
            PROTAGONIST:
            - Name: $protagonistName
            - Age: $protagonistAge years old
            
            SUPPORTING CHARACTERS:
            $characterListStr
            
            STORY HISTORY SUMMARY SO FAR:
            $historySummary
            
            USER'S CHOSEN DIRECTION TO REACH PAGE $pageNumber:
            ${chosenChoice ?: "The story is just beginning."}
            
            PACING DIRECTIVE:
            $actionType
            
            Your output must be structured strictly as a parseable JSON object with these exact string fields:
            {
              "story_text": "[Write a beautiful, highly engaging, children-friendly story paragraph of around 80-120 words for page $pageNumber. Use rich sensory details.]",
              "choices": [
                "[Choice Option 1: short, exciting action sentence]",
                "[Choice Option 2: short, exciting action sentence]"
              ],
              "lighting_effect": "[Select one suitable key depending on the action: 'EXPLOSION_RED' (for danger, flames, action), 'CALM_BLUE' (for rivers, magical lights, night), 'SPOOKY_PURPLE' (for shadows, magic, mystery), 'FOREST_GREEN' (for woods, nature), 'SUNSHINE_YELLOW' (for morning, joy, treasure), 'WHITE_LIGHT' (for standard scenes)]"
            }
            
            Do not output any additional conversational remarks, markdown backticks, or text before or after the JSON. Return only the JSON object.
        """.trimIndent()
    }

    /**
     * Fast, reliable rule-based story generator used for complete offline operation
     * or when API keys are not configured. Emulates the rising action and 20+ page climax pacing perfectly.
     */
    private fun generateRuleBasedStory(pageNumber: Int): StoryPageResponse {
        val isClimax = pageNumber >= 20
        return if (isClimax) {
            StoryPageResponse(
                storyText = "Suddenly, the ground trembles! A glorious rift of starlight opens overhead as you face the final mystery of your quest. All your choices have led to this moment. The shadows fade as the dynamic magic shines bright!",
                choices = listOf("Embrace the starlight of victory", "Complete the ancient legend"),
                lightingEffect = "SUNSHINE_YELLOW"
            )
        } else {
            val storyTexts = listOf(
                "You step carefully into the Whispering Canopy, where golden fireflies dance to an invisible melody. In the distance, a friendly owl calls your name, holding a tiny keyset in its beak.",
                "The shimmering waterfall parts, revealing a hidden tunnel carved from ancient, glowing crystal. The air smells of sweet pine, and you hear the soft laughter of forest spirits.",
                "Floating stepping stones stretch across the Sky-blue Lagoon. Each stone hums with a warm musical chord as you set foot upon it, lighting up in beautiful colors.",
                "An old treasure chest wrapped in flower vines sits in the center of the clearing. It beats softly like a mechanical heart, waiting for the chosen adventurer."
            )
            val index = pageNumber % storyTexts.size
            val choices = when (index) {
                0 -> listOf("Follow the golden fireflies deep into the woods", "Ask the friendly owl for the key")
                1 -> listOf("Enter the glowing crystal tunnel", "Follow the trail of forest flowers")
                2 -> listOf("Hop across the musical stepping stones", "Sail a leaf boat across the lagoon")
                else -> listOf("Open the vine-wrapped treasure chest", "Look for clues carved in the trees")
            }
            val effects = listOf("CALM_BLUE", "FOREST_GREEN", "SPOOKY_PURPLE", "SUNSHINE_YELLOW")
            StoryPageResponse(
                storyText = storyTexts[index],
                choices = choices,
                lightingEffect = effects[index]
            )
        }
    }
}

data class StoryPageResponse(
    val storyText: String,
    val choices: List<String>,
    val lightingEffect: String
)
