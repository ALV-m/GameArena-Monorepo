package com.gamearena.booster.panels.notifications

import com.gamearena.booster.model.BoosterNotification
import com.gamearena.booster.model.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor() {

    private val _notifications = MutableStateFlow<List<BoosterNotification>>(emptyList())
    val notifications: StateFlow<List<BoosterNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun addNotification(
        title: String,
        message: String,
        type: NotificationType,
        matchId: String? = null,
        tournamentId: String? = null,
        actionUrl: String? = null
    ) {
        val notification = BoosterNotification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            matchId = matchId,
            tournamentId = tournamentId,
            actionUrl = actionUrl
        )
        _notifications.value = listOf(notification) + _notifications.value
        _unreadCount.value = _unreadCount.value + 1
    }

    fun markAsRead(notificationId: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == notificationId) it.copy(read = true) else it
        }
        _unreadCount.value = (_unreadCount.value - 1).coerceAtLeast(0)
    }

    fun markAllAsRead() {
        _notifications.value = _notifications.value.map { it.copy(read = true) }
        _unreadCount.value = 0
    }

    fun removeNotification(notificationId: String) {
        val wasUnread = _notifications.value.find { it.id == notificationId }?.read == false
        _notifications.value = _notifications.value.filter { it.id != notificationId }
        if (wasUnread) _unreadCount.value = (_unreadCount.value - 1).coerceAtLeast(0)
    }

    fun clearAll() {
        _notifications.value = emptyList()
        _unreadCount.value = 0
    }

    fun getRecentNotifications(count: Int = 10): List<BoosterNotification> {
        return _notifications.value.take(count)
    }
}
