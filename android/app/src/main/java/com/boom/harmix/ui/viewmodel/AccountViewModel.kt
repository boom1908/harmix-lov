package com.boom.harmix.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boom.harmix.auth.AccountSlot
import com.boom.harmix.auth.GoogleAccountInfo
import com.boom.harmix.auth.GoogleAccountsRepository
import com.boom.harmix.core.NetworkMonitor
import com.boom.harmix.core.OfflineException
import com.boom.harmix.sync.YtMusicSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Done(val playlists: Int, val songs: Int) : SyncState()
    data class Failed(val message: String, val offline: Boolean) : SyncState()
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accounts: GoogleAccountsRepository,
    private val ytSync: YtMusicSyncRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val mainAccount: StateFlow<GoogleAccountInfo?> = accounts.mainAccount
    val ytAccount: StateFlow<GoogleAccountInfo?> = accounts.ytAccount

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun signInIntent(slot: AccountSlot): Intent = accounts.signInIntent(slot)

    fun onSignInResult(slot: AccountSlot, data: Intent?) {
        val info = accounts.handleSignInResult(slot, data)
        if (info != null && slot == AccountSlot.YT_SYNC) {
            // Playlists sync the moment the YouTube account connects.
            syncNow()
        }
    }

    fun signOut(slot: AccountSlot) {
        accounts.signOut(slot)
        if (slot == AccountSlot.YT_SYNC) _syncState.value = SyncState.Idle
    }

    fun syncNow() {
        if (_syncState.value is SyncState.Syncing) return
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            _syncState.value = try {
                val summary = ytSync.syncPlaylists()
                SyncState.Done(summary.playlists, summary.songs)
            } catch (e: OfflineException) {
                SyncState.Failed(e.message ?: "You're offline.", offline = true)
            } catch (e: Exception) {
                SyncState.Failed(e.message ?: "Sync failed.", offline = false)
            }
        }
    }
}