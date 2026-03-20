package dev.materii.gloom.ui.screen.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import dev.materii.gloom.api.dto.ai.ChatMessage
import dev.materii.gloom.api.service.ai.AIService
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import org.koin.compose.koinInject

class AICodeReviewScreen(
    private val filePath: String,
    private val fileContent: String,
) : Screen {

    override val key: ScreenKey get() = "ai-review-$filePath"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val aiService      = koinInject<AIService>()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        var reviewText by remember { mutableStateOf("") }
        var isLoading  by remember { mutableStateOf(true) }
        var error      by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(filePath) {
            val ext     = filePath.substringAfterLast('.', "")
            val lang    = when (ext) {
                "kt", "kts" -> "Kotlin"; "java" -> "Java"; "xml" -> "XML"
                "py" -> "Python"; "js", "ts" -> "JavaScript/TypeScript"
                else -> ext.ifBlank { "code" }
            }
            val preview  = fileContent.lines().take(200).joinToString("\n")
            val messages = listOf(
                ChatMessage("system", "You are an expert code reviewer. Be concise and use Markdown."),
                ChatMessage("user", "Review this $lang file (`$filePath`):\n\n```$ext\n$preview\n```\n\nFocus on bugs, performance, Kotlin idioms, and security. Under 400 words.")
            )
            val result = aiService.chat(messages = messages, maxTokens = 1024)
            when (result) {
                is ApiResponse.Success ->
                    reviewText = result.data.choices.firstOrNull()?.message?.content?.trim() ?: ""
                else -> error = "AI review failed. Check your API key in Settings → AI Settings."
            }
            isLoading = false
        }

        Scaffold(
            topBar = { LargeToolbar(title = "AI Code Review", scrollBehavior = scrollBehavior) },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { pv ->
            when {
                isLoading -> Box(Modifier.fillMaxSize().padding(pv), Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Box(Modifier.fillMaxSize().padding(pv), Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp))
                }
                else -> ReviewContent(filePath, reviewText, Modifier.padding(pv))
            }
        }
    }
}

/** Renders the review text with simple fenced-code-block detection. */
@Composable
private fun ReviewContent(filePath: String, text: String, modifier: Modifier = Modifier) {
    // Split on fenced code blocks manually to avoid sealed class dependency
    val segments = mutableListOf<Pair<Boolean, String>>() // isCode, content
    val fenceRegex = Regex("```(\\w*)\\n([\\s\\S]*?)```", RegexOption.MULTILINE)
    var last = 0
    for (m in fenceRegex.findAll(text)) {
        if (m.range.first > last)
            segments += false to text.substring(last, m.range.first).trim()
        segments += true to m.groupValues[2].trimEnd()
        last = m.range.last + 1
    }
    if (last < text.length) segments += false to text.substring(last).trim()
    if (segments.isEmpty()) segments += false to text

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(filePath, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp))
        }
        items(segments.size) { i ->
            val (isCode, content) = segments[i]
            if (isCode) {
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E2E))
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(content, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                        color = Color(0xFFCDD6F4), softWrap = false)
                }
            } else {
                Text(content, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}
