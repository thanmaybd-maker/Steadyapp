package com.thanu.steady.platform.connected

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GeminiProvider {

    private fun performPostRequest(urlString: String, requestJson: JSONObject): Result<String> {
        return try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true
            connection.connectTimeout = 60000
            connection.readTimeout = 60000

            connection.outputStream.use { os ->
                val input = requestJson.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            val isSuccess = responseCode in 200..299

            val stream = if (isSuccess) connection.inputStream else connection.errorStream
            val bodyStr = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            connection.disconnect()

            if (!isSuccess) {
                return Result.failure(Exception("Gemini API error ($responseCode): $bodyStr"))
            }

            val responseJson = JSONObject(bodyStr)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val textBuilder = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        textBuilder.append(part.optString("text", ""))
                    }
                }

                val groundingMetadata = candidate.optJSONObject("groundingMetadata")
                val webQueries = groundingMetadata?.optJSONArray("webSearchQueries")
                if (webQueries != null && webQueries.length() > 0) {
                    textBuilder.append("\n\n🔍 *Verified with Google Search: ")
                    for (q in 0 until webQueries.length()) {
                        textBuilder.append("\"${webQueries.getString(q)}\"")
                        if (q < webQueries.length() - 1) textBuilder.append(", ")
                    }
                    textBuilder.append("*")
                }

                val resultText = textBuilder.toString().trim()
                if (resultText.isNotEmpty()) {
                    Result.success(resultText)
                } else {
                    Result.failure(Exception("Empty response received from Gemini."))
                }
            } else {
                Result.failure(Exception("No candidate content received from Gemini."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateFoodOrStudyWithSearch(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = "MY_GEMINI_API_KEY"
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured.")
            )
        }

        val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    val parts = JSONArray().apply { put(JSONObject().put("text", prompt)) }
                    put("parts", parts)
                })
            }
            put("contents", contents)

            val tools = JSONArray().apply { put(JSONObject().apply { put("googleSearch", JSONObject()) }) }
            put("tools", tools)

            val genConfig = JSONObject().apply { put("temperature", 0.7) }
            put("generationConfig", genConfig)
        }
        performPostRequest(urlString, requestJson)
    }

    suspend fun generateDeepThinkingReview(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = "MY_GEMINI_API_KEY"
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"
        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    val parts = JSONArray().apply { put(JSONObject().put("text", prompt)) }
                    put("parts", parts)
                })
            }
            put("contents", contents)

            val genConfig = JSONObject().apply {
                val thinkingConfig = JSONObject().apply { put("thinkingLevel", "HIGH") }
                put("thinkingConfig", thinkingConfig)
            }
            put("generationConfig", genConfig)
        }
        performPostRequest(urlString, requestJson)
    }

    suspend fun generateFastBreakdown(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = "MY_GEMINI_API_KEY"
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=$apiKey"
        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    val parts = JSONArray().apply { put(JSONObject().put("text", prompt)) }
                    put("parts", parts)
                })
            }
            put("contents", contents)

            val genConfig = JSONObject().apply { put("temperature", 0.5) }
            put("generationConfig", genConfig)
        }
        performPostRequest(urlString, requestJson)
    }
}
