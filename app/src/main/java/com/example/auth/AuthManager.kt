package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        FirebaseDiagnostics.recordSessionError(context, "AutoSignIn", "resolve_default_web_client_id", e)
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val activityContext = context.findActivity() ?: context
            val result = withTimeoutOrNull(2500L) {
                credentialManager.getCredential(activityContext, request)
            }
            if (result == null) {
                onUnauthenticated()
                return@launch
            }
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: NoCredentialException) {
            FirebaseDiagnostics.recordSessionError(context, "AutoSignIn", "CredentialManager.getCredential", e)
            onUnauthenticated()
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Silent Google Sign-In cancelled or dismissed: ${e.message}", e)
            FirebaseDiagnostics.recordSessionError(context, "AutoSignIn", "CredentialManager.getCredential", e)
            onUnauthenticated()
        } catch (e: Exception) {
            Log.w("Auth", "Silent Google Sign-In unavailable: ${e.message}")
            FirebaseDiagnostics.recordSessionError(context, "AutoSignIn", "CredentialManager.getCredential", e)
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        FirebaseDiagnostics.recordSessionError(context, "GoogleSignIn", "resolve_default_web_client_id", e)
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context.findActivity() ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            FirebaseDiagnostics.recordSessionError(context, "GoogleSignIn", "GetSignInWithGoogleOption", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Play Services Sign-In failed, trying Browser OAuth fallback", e)
            FirebaseDiagnostics.recordSessionError(context, "GoogleSignIn", "GetSignInWithGoogleOption", e)
            val activity = context.findActivity()
            if (activity != null) {
                signInWithGoogleWebOAuth(
                    activity = activity,
                    onAuthSuccess = onAuthSuccess,
                    onAuthError = { webErr ->
                        onAuthError(
                            "Emulator Google Play Services has no signed-in Google account ($webErr). " +
                                "Tap 'Play in Preview Mode' or 'Diagnostics' below."
                        )
                    },
                    scope = scope
                )
            } else {
                onAuthError(
                    e.localizedMessage
                        ?: "Sign in failed. Tap 'Play in Preview Mode' or 'Diagnostics' below."
                )
            }
        }
    }
}

fun signInWithGoogleWebOAuth(
    activity: Activity,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            val provider = OAuthProvider.newBuilder("google.com")
                .addCustomParameter("prompt", "select_account")
                .build()
            val pendingResultTask = Firebase.auth.pendingAuthResult
            if (pendingResultTask != null) {
                pendingResultTask.await()
            } else {
                Firebase.auth.startActivityForSignInWithProvider(activity, provider).await()
            }
            onAuthSuccess()
        } catch (e: Exception) {
            Log.e("Auth", "Browser Google OAuth failed", e)
            FirebaseDiagnostics.recordSessionError(activity, "BrowserOAuth", "startActivityForSignInWithProvider", e)
            onAuthError(e.localizedMessage ?: "Browser Google OAuth unavailable in this preview environment")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}
