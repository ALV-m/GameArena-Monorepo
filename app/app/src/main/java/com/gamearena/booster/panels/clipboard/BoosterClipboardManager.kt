package com.gamearena.booster.panels.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.gamearena.booster.model.ClipboardEntry
import com.gamearena.booster.model.ClipboardType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val _entries = MutableStateFlow<List<ClipboardEntry>>(emptyList())
    val entries: StateFlow<List<ClipboardEntry>> = _entries.asStateFlow()

    fun saveEntry(label: String, value: String, type: ClipboardType, matchId: String? = null) {
        val entry = ClipboardEntry(
            id = UUID.randomUUID().toString(),
            label = label,
            value = value,
            type = type,
            createdAt = System.currentTimeMillis(),
            matchId = matchId
        )
        _entries.value = listOf(entry) + _entries.value
    }

    fun saveRoomCode(code: String, matchId: String? = null) {
        saveEntry("Room Code", code, ClipboardType.ROOM_CODE, matchId)
    }

    fun savePassword(password: String, matchId: String? = null) {
        saveEntry("Password", password, ClipboardType.PASSWORD, matchId)
    }

    fun saveLink(url: String, matchId: String? = null) {
        saveEntry("Link", url, ClipboardType.LINK, matchId)
    }

    fun copyToClipboard(entry: ClipboardEntry) {
        val clip = ClipData.newPlainText(entry.label, entry.value)
        clipboard.setPrimaryClip(clip)
    }

    fun copyToClipboard(label: String, value: String) {
        val clip = ClipData.newPlainText(label, value)
        clipboard.setPrimaryClip(clip)
    }

    fun removeEntry(entryId: String) {
        _entries.value = _entries.value.filter { it.id != entryId }
    }

    fun clearAll() {
        _entries.value = emptyList()
    }

    fun clearForMatch(matchId: String) {
        _entries.value = _entries.value.filter { it.matchId != matchId }
    }
}
