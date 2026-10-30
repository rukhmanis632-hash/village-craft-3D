package com.example.auth

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.credentials.CredentialManager
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CheckStatus {
    PASS,
    WARN,
    FAIL
}

data class DiagnosticCheckItem(
    val category: String,
    val title: String,
    val status: CheckStatus,
    val detail: String
)

data class NetworkEndpointProbe(
    val name: String,
    val url: String,
    val reachable: Boolean,
    val httpStatus: Int,
    val latencyMs: Long,
    val errorDetail: String? = null
)

data class SessionErrorEvent(
    val timestampFormatted: String,
    val subsystem: String,
    val operation: String,
    val exceptionType: String,
    val errorCode: String,
    val message: String,
    val networkStateSummary: String,
    val remediationHint: String
)

data class DiagnosticReport(
    val generatedAt: String,
    val packageName: String,
    val sha1Fingerprint: String,
    val sha256Fingerprint: String,
    val checks: List<DiagnosticCheckItem>,
    val endpointProbes: List<NetworkEndpointProbe>,
    val recentErrors: List<SessionErrorEvent>,
    val rawJsonReport: String
)

object FirebaseDiagnostics {
    private const val TAG = "FirebaseDiag"
    private val sessionErrors = mutableStateListOf<SessionErrorEvent>()

    fun getRecentSessionErrors(): List<SessionErrorEvent> = sessionErrors.toList()

    fun clearSessionErrors() {
        sessionErrors.clear()
    }

    /**
     * Records and logs detailed network/auth error information for the current user session.
     */
    fun recordSessionError(
        context: Context,
        subsystem: String,
        operation: String,
        throwable: Throwable
    ) {
        val nowStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val netSummary = inspectConnectivitySnapshot(context)
        val exceptionType = throwable::class.java.simpleName.ifBlank { "Exception" }
        val errorCode = extractErrorCode(throwable)
        val hint = buildRemediationHint(throwable, netSummary)

        val event = SessionErrorEvent(
            timestampFormatted = nowStr,
            subsystem = subsystem,
            operation = operation,
            exceptionType = exceptionType,
            errorCode = errorCode,
            message = throwable.localizedMessage ?: throwable.message ?: "Unknown error",
            networkStateSummary = netSummary,
            remediationHint = hint
        )

        sessionErrors.add(0, event)
        if (sessionErrors.size > 25) {
            sessionErrors.removeAt(sessionErrors.lastIndex)
        }

        val currentUser = try {
            Firebase.auth.currentUser
        } catch (_: Exception) {
            null
        }

        val errorJson = JSONObject().apply {
            put("timestamp", nowStr)
            put("subsystem", subsystem)
            put("operation", operation)
            put("exceptionType", exceptionType)
            put("errorCode", errorCode)
            put("message", event.message)
            put("cause", throwable.cause?.let { "${it::class.java.simpleName}: ${it.message}" } ?: JSONObject.NULL)
            put("networkState", netSummary)
            put("remediationHint", hint)
            put(
                "sessionUser",
                JSONObject().apply {
                    put("isAuthenticated", currentUser != null)
                    put("uid", currentUser?.uid ?: JSONObject.NULL)
                    put("email", currentUser?.email ?: JSONObject.NULL)
                    put(
                        "providers",
                        JSONArray(currentUser?.providerData?.map { it.providerId } ?: emptyList<String>())
                    )
                }
            )
        }

        Log.e(TAG, "SESSION_ERROR_DIAGNOSTIC: $errorJson", throwable)
    }

    /**
     * Runs a full diagnostic audit of Firebase App, Firebase Auth, Google Sign-In / Credential Manager,
     * Firestore named database configuration, APK signing certificates, and live HTTPS network endpoints.
     */
    suspend fun runFullDiagnostics(context: Context): DiagnosticReport = withContext(Dispatchers.IO) {
        val checks = mutableListOf<DiagnosticCheckItem>()
        val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val pkgName = context.packageName
        val (sha1, sha256) = getApkSigningFingerprints(context)

        // 1. Check FirebaseApp Initialization
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isEmpty()) {
                checks.add(
                    DiagnosticCheckItem(
                        category = "Firebase Core",
                        title = "FirebaseApp Initialization",
                        status = CheckStatus.FAIL,
                        detail = "No FirebaseApp instances initialized. Verify google-services.json."
                    )
                )
            } else {
                val defaultApp = FirebaseApp.getInstance()
                val opts = defaultApp.options
                val hasProjectId = !opts.projectId.isNullOrBlank()
                val hasAppId = opts.applicationId.isNotBlank()
                val hasApiKey = opts.apiKey.isNotBlank()
                checks.add(
                    DiagnosticCheckItem(
                        category = "Firebase Core",
                        title = "FirebaseApp & Options",
                        status = if (hasProjectId && hasAppId && hasApiKey) CheckStatus.PASS else CheckStatus.WARN,
                        detail = "projectId=${opts.projectId}, appId=${opts.applicationId.take(24)}..., apiKeyPresent=$hasApiKey"
                    )
                )
            }
        } catch (e: Exception) {
            recordSessionError(context, "FirebaseCore", "FirebaseApp.getInstance", e)
            checks.add(
                DiagnosticCheckItem(
                    category = "Firebase Core",
                    title = "FirebaseApp Initialization",
                    status = CheckStatus.FAIL,
                    detail = "${e::class.java.simpleName}: ${e.localizedMessage}"
                )
            )
        }

        // 2. Check FirebaseAuth & Current User Session
        try {
            val auth = Firebase.auth
            val user = auth.currentUser
            if (user != null) {
                val providers = user.providerData.joinToString(", ") { it.providerId }
                checks.add(
                    DiagnosticCheckItem(
                        category = "Firebase Auth",
                        title = "Auth SDK & Active Session",
                        status = CheckStatus.PASS,
                        detail = "Signed in as uid=${user.uid.take(10)}... (${user.email ?: user.displayName ?: "No email"}), providers=[$providers]"
                    )
                )
            } else {
                checks.add(
                    DiagnosticCheckItem(
                        category = "Firebase Auth",
                        title = "Auth SDK & Active Session",
                        status = CheckStatus.PASS,
                        detail = "FirebaseAuth initialized. Current session: Unauthenticated (user == null)."
                    )
                )
            }
        } catch (e: Exception) {
            recordSessionError(context, "FirebaseAuth", "Firebase.auth", e)
            checks.add(
                DiagnosticCheckItem(
                    category = "Firebase Auth",
                    title = "Auth SDK Initialization",
                    status = CheckStatus.FAIL,
                    detail = "${e::class.java.simpleName}: ${e.localizedMessage}"
                )
            )
        }

        // 3. Check Google Sign-In Web Client ID & CredentialManager Dependencies
        try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val looksValid = webClientId.isNotBlank() && webClientId.contains("googleusercontent")
            val maskedClientId = if (webClientId.length > 18) {
                "${webClientId.take(14)}...${webClientId.takeLast(8)}"
            } else {
                webClientId
            }

            CredentialManager.create(context)
            GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build()
            GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(true)
                .build()

            checks.add(
                DiagnosticCheckItem(
                    category = "Google Sign-In",
                    title = "CredentialManager & Web Client ID",
                    status = if (looksValid) CheckStatus.PASS else CheckStatus.WARN,
                    detail = "CredentialManager & GoogleId options initialized. default_web_client_id=$maskedClientId"
                )
            )
        } catch (e: Exception) {
            recordSessionError(context, "GoogleSignIn", "CredentialManager/WebClientId", e)
            checks.add(
                DiagnosticCheckItem(
                    category = "Google Sign-In",
                    title = "CredentialManager & Web Client ID",
                    status = CheckStatus.FAIL,
                    detail = "${e::class.java.simpleName}: ${e.localizedMessage}"
                )
            )
        }

        // 4. Check Google Play Services (com.google.android.gms) on Device/Emulator
        try {
            val pm = context.packageManager
            val gmsInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo("com.google.android.gms", PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo("com.google.android.gms", 0)
            }
            val versionName = gmsInfo.versionName ?: "unknown"
            checks.add(
                DiagnosticCheckItem(
                    category = "Play Services",
                    title = "Google Play Services Package",
                    status = CheckStatus.PASS,
                    detail = "Installed version=$versionName (Note: Emulator preview has no OS Google Account signed in by default)"
                )
            )
        } catch (e: Exception) {
            checks.add(
                DiagnosticCheckItem(
                    category = "Play Services",
                    title = "Google Play Services Package",
                    status = CheckStatus.WARN,
                    detail = "Google Play Services not found (${e::class.java.simpleName}). Use Browser OAuth or Preview Mode."
                )
            )
        }

        // 5. Check APK Signing Certificate Fingerprints
        checks.add(
            DiagnosticCheckItem(
                category = "App Signature",
                title = "APK Signing Certificate (SHA-1)",
                status = if (sha1 != "Unavailable") CheckStatus.PASS else CheckStatus.WARN,
                detail = "pkg=$pkgName | SHA-1=$sha1"
            )
        )

        // 6. Check Named Firestore Database Configuration
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            checks.add(
                DiagnosticCheckItem(
                    category = "Firestore",
                    title = "Named Database Initialization",
                    status = if (dbId.isNotBlank()) CheckStatus.PASS else CheckStatus.FAIL,
                    detail = "databaseId=$dbId (instance=${db.app.name})"
                )
            )
        } catch (e: Exception) {
            recordSessionError(context, "Firestore", "FirebaseFirestore.getInstance", e)
            checks.add(
                DiagnosticCheckItem(
                    category = "Firestore",
                    title = "Named Database Initialization",
                    status = CheckStatus.FAIL,
                    detail = "${e::class.java.simpleName}: ${e.localizedMessage}"
                )
            )
        }

        // 7. Check Active Network & Capabilities
        val netSnapshot = inspectConnectivitySnapshot(context)
        val hasInternet = netSnapshot.contains("INTERNET=true")
        val isValidated = netSnapshot.contains("VALIDATED=true")
        checks.add(
            DiagnosticCheckItem(
                category = "Network",
                title = "Android ConnectivityManager",
                status = when {
                    hasInternet && isValidated -> CheckStatus.PASS
                    hasInternet -> CheckStatus.WARN
                    else -> CheckStatus.FAIL
                },
                detail = netSnapshot
            )
        )

        // 8. Probe Live Google OAuth & Firebase Auth Endpoints
        val endpoints = listOf(
            "Google Connectivity (204)" to "https://www.gstatic.com/generate_204",
            "Google OAuth OpenID Config" to "https://accounts.google.com/.well-known/openid-configuration",
            "Firebase Auth IdentityToolkit" to "https://identitytoolkit.googleapis.com"
        )
        val probes = endpoints.map { (name, url) ->
            probeHttpsEndpoint(context, name, url)
        }

        val rawJson = buildJsonReport(
            generatedAt = timeFmt,
            packageName = pkgName,
            sha1 = sha1,
            sha256 = sha256,
            checks = checks,
            probes = probes,
            errors = sessionErrors.toList()
        )

        Log.i(TAG, "FIREBASE_DIAGNOSTIC_REPORT: $rawJson")

        DiagnosticReport(
            generatedAt = timeFmt,
            packageName = pkgName,
            sha1Fingerprint = sha1,
            sha256Fingerprint = sha256,
            checks = checks,
            endpointProbes = probes,
            recentErrors = sessionErrors.toList(),
            rawJsonReport = rawJson
        )
    }

    private fun probeHttpsEndpoint(
        context: Context,
        name: String,
        urlString: String
    ): NetworkEndpointProbe {
        val start = System.currentTimeMillis()
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            InetAddress.getByName(url.host)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            val latency = System.currentTimeMillis() - start
            // 200..499 means DNS + TLS + TCP handshake succeeded and the server responded
            val reachable = code in 200..499
            NetworkEndpointProbe(
                name = name,
                url = urlString,
                reachable = reachable,
                httpStatus = code,
                latencyMs = latency,
                errorDetail = if (reachable) null else "HTTP $code"
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            recordSessionError(context, "NetworkProbe", "GET $urlString", e)
            NetworkEndpointProbe(
                name = name,
                url = urlString,
                reachable = false,
                httpStatus = -1,
                latencyMs = latency,
                errorDetail = "${e::class.java.simpleName}: ${e.localizedMessage ?: e.message}"
            )
        } finally {
            connection?.disconnect()
        }
    }

    fun inspectConnectivitySnapshot(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "ConnectivityManager unavailable"
            val activeNet = cm.activeNetwork ?: return "No active network (offline)"
            val caps = cm.getNetworkCapabilities(activeNet)
                ?: return "Active network has no NetworkCapabilities"
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val notMetered = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

            val transports = mutableListOf<String>()
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) transports.add("WIFI")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) transports.add("CELLULAR")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) transports.add("ETHERNET")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) transports.add("VPN")

            val linkProps = cm.getLinkProperties(activeNet)
            val dnsCount = linkProps?.dnsServers?.size ?: 0

            "Transports=${transports.ifEmpty { listOf("OTHER") }} | INTERNET=$hasInternet | VALIDATED=$isValidated | UNMETERED=$notMetered | DNS_SERVERS=$dnsCount"
        } catch (e: Exception) {
            "Connectivity check error: ${e::class.java.simpleName}: ${e.message}"
        }
    }

    private fun getApkSigningFingerprints(context: Context): Pair<String, String> {
        return try {
            val pm = context.packageManager
            val bytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = pm.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = pkgInfo.signingInfo
                val sigs = if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else {
                    null
                }
                sigs?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                pkgInfo.signatures?.firstOrNull()?.toByteArray()
            }

            if (bytes == null) {
                "Unavailable" to "Unavailable"
            } else {
                formatDigest(bytes, "SHA-1") to formatDigest(bytes, "SHA-256")
            }
        } catch (e: Exception) {
            "Unavailable (${e::class.java.simpleName})" to "Unavailable"
        }
    }

    private fun formatDigest(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(bytes)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    private fun extractErrorCode(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseAuthException -> throwable.errorCode
            is FirebaseFirestoreException -> throwable.code.name
            is FirebaseNetworkException -> "FIREBASE_NETWORK_ERROR"
            else -> throwable::class.java.simpleName.ifBlank { "UNKNOWN_ERROR" }
        }
    }

    private fun buildRemediationHint(throwable: Throwable, netSummary: String): String {
        val msg = (throwable.message ?: "").lowercase(Locale.US)
        val cls = throwable::class.java.simpleName
        return when {
            !netSummary.contains("INTERNET=true") ->
                "Device/emulator reports no active internet connection. Check emulator Wi-Fi/Cellular status or use Local Preview Mode."
            cls.contains("GetCredentialCancellationException") ->
                "Credential Manager sheet was dismissed or Google Play Services failed to add an OS account in the preview emulator. Use Browser OAuth or Local Preview Mode."
            cls.contains("NoCredentialException") ->
                "No Google account is signed into Android OS Settings on this emulator/device. Sign in interactively or use Local Preview Mode."
            cls.contains("GetCredentialProviderConfigurationException") ->
                "Google Play Services Credential Provider is missing or outdated on this device image."
            throwable is FirebaseNetworkException || msg.contains("network") || msg.contains("unreachable") || msg.contains("timeout") ->
                "Network request to Google/Firebase timed out or was blocked by container firewall. Check HTTPS endpoint probes in Diagnostics."
            msg.contains("developer_error") || msg.contains("10:") || msg.contains("12500") ->
                "OAuth SHA-1 or package name mismatch. Compare the runtime SHA-1 in Diagnostics against google-services.json."
            else ->
                "Inspect Logcat tag 'FirebaseDiag' for the full stack trace and session JSON payload."
        }
    }

    private fun buildJsonReport(
        generatedAt: String,
        packageName: String,
        sha1: String,
        sha256: String,
        checks: List<DiagnosticCheckItem>,
        probes: List<NetworkEndpointProbe>,
        errors: List<SessionErrorEvent>
    ): String {
        val root = JSONObject()
        root.put("generatedAt", generatedAt)
        root.put("packageName", packageName)
        root.put("sha1Fingerprint", sha1)
        root.put("sha256Fingerprint", sha256)

        val checksArray = JSONArray()
        checks.forEach { item ->
            checksArray.put(
                JSONObject().apply {
                    put("category", item.category)
                    put("title", item.title)
                    put("status", item.status.name)
                    put("detail", item.detail)
                }
            )
        }
        root.put("checks", checksArray)

        val probesArray = JSONArray()
        probes.forEach { probe ->
            probesArray.put(
                JSONObject().apply {
                    put("name", probe.name)
                    put("url", probe.url)
                    put("reachable", probe.reachable)
                    put("httpStatus", probe.httpStatus)
                    put("latencyMs", probe.latencyMs)
                    put("errorDetail", probe.errorDetail ?: JSONObject.NULL)
                }
            )
        }
        root.put("endpointProbes", probesArray)

        val errorsArray = JSONArray()
        errors.forEach { err ->
            errorsArray.put(
                JSONObject().apply {
                    put("timestamp", err.timestampFormatted)
                    put("subsystem", err.subsystem)
                    put("operation", err.operation)
                    put("exceptionType", err.exceptionType)
                    put("errorCode", err.errorCode)
                    put("message", err.message)
                    put("networkState", err.networkStateSummary)
                    put("remediationHint", err.remediationHint)
                }
            )
        }
        root.put("recentSessionErrors", errorsArray)
        return root.toString()
    }
}
