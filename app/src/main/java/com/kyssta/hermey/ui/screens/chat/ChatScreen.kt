package com.kyssta.hermey.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.networking.ChatMessage
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.ChatViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onNavigateBack: () -> Unit
) {
    val chatVm: ChatViewModel = viewModel()
    val messages by chatVm.messages.collectAsState()
    val input by chatVm.input.collectAsState()
    val streaming by chatVm.streaming.collectAsState()
    val loading by chatVm.loading.collectAsState()
    val error by chatVm.error.collectAsState()
    val listState = rememberLazyListState()

    // Blank id = new chat; non-blank = load the stored transcript once.
    LaunchedEffect(sessionId) {
        if (sessionId.isEmpty()) chatVm.startNewChat() else chatVm.loadSession(sessionId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatVm.currentTitle.collectAsState().value ?: "Chat") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HermesColors.Surface,
                    titleContentColor = HermesColors.OnSurface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                input = input,
                onInputChanged = { chatVm.updateInput(it) },
                onSend = { chatVm.sendMessage() },
                streaming = streaming,
                onCancel = { chatVm.cancelStream() }
            )
        }
    ) { padding ->
        if (loading && messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg -> MessageBubble(message = msg) }
                if (streaming) item { StreamingIndicator() }
                error?.let { item { Text(it, color = HermesColors.Error, style = MaterialTheme.typography.bodySmall) } }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    input: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    streaming: Boolean,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HermesColors.Surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 4,
                enabled = !streaming
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (streaming) {
                IconButton(onClick = onCancel) {
                    Text("■", style = MaterialTheme.typography.headlineSmall, color = HermesColors.Error)
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = input.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (input.isNotBlank()) HermesColors.Primary else HermesColors.Outline
                    )
                ) {
                    Text("➤", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) HermesColors.Primary else HermesColors.Glass
        )
    ) {
        Text(
            text = message.content ?: "",
            modifier = Modifier.padding(12.dp),
            color = if (isUser) HermesColors.OnPrimary else HermesColors.OnSurface
        )
    }
}

@Composable
fun StreamingIndicator() {
    Card(colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI is thinking...", color = HermesColors.OnSurfaceVariant)
        }
    }
}
