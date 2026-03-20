package dev.materii.gloom.ui.screen.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import dev.materii.gloom.api.dto.ai.ChatMessage
import dev.materii.gloom.api.dto.chat.MessageSegment
import dev.materii.gloom.api.dto.chat.parseSegments
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
        val aiService  = koinInject<AIService>()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        var segments   by remember { mutableStateOf<List<MessageSegment>>(emptyList()) }
        var isLoading  by remember { mutableStateOf(true) }
        var error      by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(filePath) {
            val ext = filePath.substringAfterLast('.', "")
            val lang = when (ext) {
                "kt", "kts" -> "Kotlin"
                "java"      -> "Java"
                "xml"       -> "XML"
                "py"        -> "Python"
                "js", "ts"  -> "JavaScript/TypeScript"
                else        -> ext.ifBlank { "code" }
            }
            val preview = fileContent.lines().take(200).joinToString("\n")
            val messages = listOf(
                ChatMessage("system", "You are an expert code reviewer. Provide concise, actionable feedback with specific line references where possible. Use Markdown with code blocks."),
                ChatMessage("user", "Please review this $lang file (`$filePath`):\n\n```$ext\n$preview\n```\n\nFocus on: bugs, performance, readability, Kotlin idioms, and security. Keep it under 400 words.")
            )
            val result = aiService.chat(messages = messages, maxTokens = 1024)
            when (result) {
                is ApiResponse.Success -> {
                    val text = result.data.choices.firstOrNull()?.message?.content?.trim() ?: ""
                    segments = parseSegments(text)
                }
                else -> error = "AI review failed. Check your API key in Settings → AI Settings."
            }
            isLoading = false
        }

        Scaffold(
            topBar = {
                LargeToolbar(
                    title          = "AI Code Review",
                    scrollBehavior = scrollBehavior,
                )
            },
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
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(pv).padding(16.dp)
                ) {
                    item {
                        Text(
                            filePath,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    items(segments) { seg ->
                        when (seg) {
                            is MessageSegment.Text -> Text(
                                seg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            is MessageSegment.Code -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E1E2E))
                                        .padding(12.dp)
                                ) {
                                    if (seg.language.isNotBlank()) {
                                        Text(
                                            seg.language,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFCDD6F4),
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(bottom = 6.dp),
                                        )
                                    }
                                    Text(
                                        seg.code,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        ),
                                        color = Color(0xFFCDD6F4),
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
