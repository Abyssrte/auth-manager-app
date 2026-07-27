package com.authmanager.app.network

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Result of a file fetch: content plus the blob SHA (needed to overwrite it later). */
data class RemoteFile(val content: String, val sha: String)

/** Thrown for any GitHub API failure, with a message safe to show in the UI. */
class GitHubException(message: String) : IOException(message)

/**
 * Talks directly to the GitHub Contents API (api.github.com) — no CDN caching to
 * fight with, unlike raw.githubusercontent.com, so every read reflects the latest
 * commit immediately.
 */
object GitHubClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun contentsUrl(path: String) =
        "https://api.github.com/repos/${GitHubConfig.OWNER}/${GitHubConfig.REPO}/contents/$path?ref=${GitHubConfig.BRANCH}"

    private fun authedRequest(url: String): Request.Builder {
        val token = SecretConfig.getToken()
            ?: throw GitHubException(SecretConfig.missingTokenMessage())
        return Request.Builder()
            .url(url)
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .addHeader("User-Agent", "auth-manager-app")
    }

    /** Fetch a file's raw text content + sha. Throws GitHubException on any failure. */
    suspend fun fetchFile(path: String): RemoteFile = withContext(Dispatchers.IO) {
        val request = authedRequest(contentsUrl(path)).get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw GitHubException("Fetch failed for $path: HTTP ${resp.code}")
            }
            val body = resp.body?.string() ?: throw GitHubException("Empty response for $path")
            val json = JSONObject(body)
            val encoded = json.getString("content").replace("\n", "")
            val decoded = String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
            RemoteFile(content = decoded, sha = json.getString("sha"))
        }
    }

    /**
     * Overwrite a file's content with a new commit. Needs the current sha (from
     * fetchFile) so GitHub knows this isn't a stale write.
     */
    suspend fun writeFile(path: String, newContent: String, sha: String, commitMessage: String) =
        withContext(Dispatchers.IO) {
            val encoded = Base64.encodeToString(newContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val payload = JSONObject().apply {
                put("message", commitMessage)
                put("content", encoded)
                put("sha", sha)
                put("branch", GitHubConfig.BRANCH)
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = authedRequest(contentsUrl(path)).put(body).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string().orEmpty()
                    throw GitHubException("Write failed for $path: HTTP ${resp.code} $errBody")
                }
            }
        }
}
