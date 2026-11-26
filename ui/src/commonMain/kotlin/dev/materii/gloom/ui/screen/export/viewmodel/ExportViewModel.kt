package dev.materii.gloom.ui.screen.export.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.domain.manager.BuildAlerts
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.coroutines.cancellation.CancellationException

/** Exports the user's own GitHub data as JSON text, handed to the share sheet. */
class ExportViewModel(
    private val repo: GithubRepository,
    private val alerts: BuildAlerts?,
) : ScreenModel {

    enum class Kind(val title: String, val description: String) {
        STARRED("Starred repositories", "Everything you've starred, with description, language and star count"),
        REPOS("Your repositories", "Repositories you own or collaborate on"),
        GISTS("Your gists", "Gist ids, descriptions, file names and links"),
        ALERTS("Build alert watch list", "Repositories you get failed-build alerts for"),
    }

    var busy by mutableStateOf<Kind?>(null)
        private set

    /** Ready-to-share text; the screen hands it to the share sheet and then calls [consume]. */
    var payload by mutableStateOf<String?>(null)
        private set

    var message by mutableStateOf<String?>(null)

    val kinds: List<Kind> get() = Kind.entries.filter { it != Kind.ALERTS || alerts != null }

    private val pretty = Json { prettyPrint = true }

    fun export(kind: Kind) {
        if (busy != null) return
        screenModelScope.launch {
            busy = kind
            try {
                val items = when (kind) {
                    Kind.STARRED -> fetchAll("user/starred") { repoSummary(it) }
                    Kind.REPOS -> fetchAll("user/repos") { repoSummary(it) }
                    Kind.GISTS -> fetchAll("gists") { gistSummary(it) }
                    Kind.ALERTS -> buildJsonArray { alerts?.watchedRepos()?.forEach { add(JsonPrimitive(it)) } }
                }
                if (items.isEmpty()) {
                    message = "Nothing to export yet"
                } else {
                    payload = toText(kind, items)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                message = e.message ?: "Export failed"
            } finally {
                busy = null
            }
        }
    }

    fun consume() {
        payload = null
    }

    private suspend fun fetchAll(endpoint: String, map: (JsonObject) -> JsonObject): JsonArray {
        val out = mutableListOf<JsonElement>()
        for (page in 1..MAX_PAGES) {
            when (val response = repo.getListPage(endpoint, page)) {
                is ApiResponse.Success -> {
                    val items = Json.parseToJsonElement(response.data).jsonArray
                    out += items.map { map(it.jsonObject) }
                    if (items.size < 100) break
                }
                is ApiResponse.Empty -> break
                is ApiResponse.Error -> error(response.error.message ?: "GitHub returned an error")
                is ApiResponse.Failure -> error("Couldn't reach GitHub. Check your connection.")
            }
        }
        return JsonArray(out)
    }

    private fun str(o: JsonObject, key: String): JsonElement =
        o[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull?.let { JsonPrimitive(it) } ?: JsonNull

    private fun repoSummary(o: JsonObject) = buildJsonObject {
        put("full_name", str(o, "full_name"))
        put("url", str(o, "html_url"))
        put("description", JsonPrimitive(o["description"]?.jsonPrimitive?.contentOrNull?.take(300)))
        put("language", str(o, "language"))
        put("stars", o["stargazers_count"] ?: JsonNull)
        put("private", o["private"] ?: JsonNull)
        put("fork", o["fork"] ?: JsonNull)
        put("updated_at", str(o, "updated_at"))
    }

    private fun gistSummary(o: JsonObject) = buildJsonObject {
        put("id", str(o, "id"))
        put("description", JsonPrimitive(o["description"]?.jsonPrimitive?.contentOrNull?.take(300)))
        put("url", str(o, "html_url"))
        put("public", o["public"] ?: JsonNull)
        put("updated_at", str(o, "updated_at"))
        put("files", buildJsonArray {
            (o["files"] as? JsonObject)?.keys?.forEach { add(JsonPrimitive(it)) }
        })
    }

    /** Share sheets choke on very large text, so long exports are trimmed to a safe size. */
    private fun toText(kind: Kind, items: JsonArray): String {
        var list = items
        var trimmed = false
        var text = render(kind, list, 0)
        while (text.length > MAX_CHARS && list.size > 1) {
            list = JsonArray(list.take(list.size * 3 / 4))
            trimmed = true
            text = render(kind, list, items.size - list.size)
        }
        return text.also { if (trimmed) message = "Export was large; the first ${list.size} of ${items.size} items are included" }
    }

    private fun render(kind: Kind, list: JsonArray, omitted: Int): String =
        pretty.encodeToString(JsonObject.serializer(), buildJsonObject {
            put("export", kind.name.lowercase())
            put("count", list.size)
            if (omitted > 0) put("omitted", omitted)
            put("items", list)
        })

    private companion object {
        const val MAX_PAGES = 10
        const val MAX_CHARS = 400_000
    }
}
