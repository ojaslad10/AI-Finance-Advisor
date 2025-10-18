package com.example.expensetracker.ui.ai

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.ui.MainViewModel
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun AiAdvisorScreen(mainViewModel: MainViewModel = viewModel()) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Observe chat state from ViewModel
    val messages = mainViewModel.chatMessages
    val sending by mainViewModel.chatSending

    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(8.dp)
    ) {
        // Chat list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }

            if (sending) {
                item {
                    ChatBubble(
                        message = ChatMessage("Thinking...", isUser = false)
                    )
                }
            }
        }

        // Input + send row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Type a message...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = false,
                maxLines = 4
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        mainViewModel.sendChatMessageFromUi(inputText) { err ->
                            // optional: show a toast
                            Log.e("AiAdvisor", "Chat send error: $err")
                        }
                        inputText = ""
                        coroutineScope.launch {
                            listState.animateScrollToItem(mainViewModel.chatMessages.size - 1)
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }


        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val arrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    val bubbleColor = if (message.isUser) Color(0xFF6366F1) else Color(0xFF2C2C2C)
    val textWeight = if (message.isUser) FontWeight.Medium else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = arrangement
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bubbleColor,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = Color.White,
                fontWeight = textWeight,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
