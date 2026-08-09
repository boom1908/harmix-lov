package com.boom.harmix.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A signed-in Google identity. Harmix keeps two of these, fully separate. */
data class GoogleAccountInfo(
    val email: String,
    val displayName: String,
    val photoUrl: String?
)

/** Which of the two independent Google slots a sign-in belongs to. */
enum class AccountSlot { MAIN, YT_SYNC }

private const val PREFS = "harmix_google_accounts"
private const val YT_SCOPE = "https://www.googleapis.com/auth/youtube.readonly"

/**
 * Web (server) OAuth client ID from Google Cloud. Required so Google Sign-In
 * issues a real ID token that Firebase / our backend can verify.
 */
const val WEB_CLIENT_ID = "308449061407-kt6ncig4t96s1fvki4384m143i33j050.apps.googleusercontent.com"

/**
 * Handles TWO completely independent Google sign-ins:
 *  - MAIN: the Harmix account (listening history, playlists, liked songs).
 *  - YT_SYNC: a throwaway/second account only used to pull YouTube Music playlists.
 *
 * They never reference each other, so the user can swap the YouTube account
 * as often as they like without touching their Harmix account.
 */
@Singleton
class GoogleAccountsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _mainAccount = MutableStateFlow(read(AccountSlot.MAIN))
    val mainAccount: StateFlow<GoogleAccountInfo?> = _mainAccount.asStateFlow()

    private val _ytAccount = MutableStateFlow(read(AccountSlot.YT_SYNC))
    val ytAccount: StateFlow<GoogleAccountInfo?> = _ytAccount.asStateFlow()

    /** Build a fresh sign-in intent. Always shows the account chooser. */
    fun signInIntent(slot: AccountSlot): Intent {
        val client = clientFor(slot)
        client.signOut() // force the chooser so a different account can be picked
        return client.signInIntent
    }

    /** Feed the Activity result back in here. Returns the account, or null if cancelled. */
    fun handleSignInResult(slot: AccountSlot, data: Intent?): GoogleAccountInfo? {
        val account = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(data).getResult(
                com.google.android.gms.common.api.ApiException::class.java
            )
        }.getOrNull() ?: return null

        val email = account.email ?: return null
        val info = GoogleAccountInfo(
            email = email,
            displayName = account.displayName ?: email.substringBefore('@'),
            photoUrl = account.photoUrl?.toString()
        )
        write(slot, info)
        // Drop the global GMS session so the two slots never bleed into each other.
        clientFor(slot).signOut()
        return info
    }

    fun signOut(slot: AccountSlot) {
        runCatching { clientFor(slot).signOut() }
        write(slot, null)
    }

    /** OAuth access token for the YouTube Data API, for the connected sync account only. */
    suspend fun youtubeAccessToken(): String? = withContext(Dispatchers.IO) {
        val email = _ytAccount.value?.email ?: return@withContext null
        runCatching {
            GoogleAuthUtil.getToken(context, Account(email, "com.google"), "oauth2:$YT_SCOPE")
        }.getOrNull()
    }

    private fun clientFor(slot: AccountSlot): GoogleSignInClient {
        val options = when (slot) {
            AccountSlot.MAIN ->
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestProfile()
                    .requestIdToken(WEB_CLIENT_ID)
                    .build()

            AccountSlot.YT_SYNC ->
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestProfile()
                    .requestIdToken(WEB_CLIENT_ID)
                    .requestScopes(Scope(YT_SCOPE))
                    .build()
        }
        return GoogleSignIn.getClient(context, options)
    }

    private fun key(slot: AccountSlot) = if (slot == AccountSlot.MAIN) "main" else "yt"

    private fun read(slot: AccountSlot): GoogleAccountInfo? {
        val k = key(slot)
        val email = prefs.getString("${k}_email", null) ?: return null
        return GoogleAccountInfo(
            email = email,
            displayName = prefs.getString("${k}_name", email.substringBefore('@')).orEmpty(),
            photoUrl = prefs.getString("${k}_photo", null)
        )
    }

    private fun write(slot: AccountSlot, info: GoogleAccountInfo?) {
        val k = key(slot)
        prefs.edit().apply {
            if (info == null) {
                remove("${k}_email"); remove("${k}_name"); remove("${k}_photo")
            } else {
                putString("${k}_email", info.email)
                putString("${k}_name", info.displayName)
                putString("${k}_photo", info.photoUrl)
            }
        }.apply()

        when (slot) {
            AccountSlot.MAIN -> _mainAccount.value = info
            AccountSlot.YT_SYNC -> _ytAccount.value = info
        }
    }
}