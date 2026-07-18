package com.gamearena.booster.panels.chat

import com.gamearena.booster.model.ChatChannel
import com.gamearena.booster.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatManager @Inject constructor() {

    private val _messages = MutableStateFlow<Map<ChatChannel, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<ChatChannel, List<ChatMessage>>> = _messages.asStateFlow()

    private val _unreadCounts = MutableStateFlow<Map<ChatChannel, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<ChatChannel, Int>> = _unreadCounts.asStateFlow()

    private val _activeChannel = MutableStateFlow(ChatChannel.GENERAL)
    val activeChannel: StateFlow<ChatChannel> = _activeChannel.asStateFlow()

    fun setActiveChannel(channel: ChatChannel) {
        _activeChannel.value = channel
        clearUnread(channel)
    }

    fun sendMessage(content: String, channel: ChatChannel, senderName: String = "You") {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = "self",
            senderName = senderName,
            content = content,
            timestamp = System.currentTimeMillis(),
            channelType = channel
        )
        addMessage(message)
    }

    fun receiveMessage(senderId: String, senderName: String, content: String, channel: ChatChannel) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = senderName,
            content = content,
            timestamp = System.currentTimeMillis(),
            channelType = channel
        )
        addMessage(message)
        if (channel != _activeChannel.value) {
            incrementUnread(channel)
        }
    }

    fun receiveSystemMessage(content: String, channel: ChatChannel) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = "system",
            senderName = "System",
            content = content,
            timestamp = System.currentTimeMillis(),
            channelType = channel,
            isSystem = true
        )
        addMessage(message)
    }

    private fun addMessage(message: ChatMessage) {
        val current = _messages.value.toMutableMap()
        val channelMessages = current[message.channelType]?.toMutableList() ?: mutableListOf()
        channelMessages.add(message)
        current[message.channelType] = channelMessages
        _messages.value = current
    }

    private fun incrementUnread(channel: ChatChannel) {
        val current = _unreadCounts.value.toMutableMap()
        current[channel] = (current[channel] ?: 0) + 1
        _unreadCounts.value = current
    }

    private fun clearUnread(channel: ChatChannel) {
        val current = _unreadCounts.value.toMutableMap()
        current[channel] = 0
        _unreadCounts.value = current
    }

    fun getChannelMessages(channel: ChatChannel): List<ChatMessage> {
        return _messages.value[channel] ?: emptyList()
    }

    fun getUnreadCount(channel: ChatChannel): Int {
        return _unreadCounts.value[channel] ?: 0
    }

    fun clearChannel(channel: ChatChannel) {
        val current = _messages.value.toMutableMap()
        current.remove(channel)
        _messages.value = current
        clearUnread(channel)
    }
}
