package com.example.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class PlayerProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Explorer Leo",
    val coins: Long = 24180L,
    val rubies: Long = 142L,
    val selectedWorld: Long = 0L,
    val selectedChar: String = "leo",
    val selectedBoard: String = "solar",
    val challengeDist: Long = 1200L,
    val challengeGems: Long = 0L,
    val magnetLevel: Long = 2L,
    val shieldLevel: Long = 2L,
    val boostLevel: Long = 1L,
    val multiplierLevel: Long = 3L,
    val highScoreW0: Long = 98420L,
    val highScoreW1: Long = 64200L,
    val highScoreW2: Long = 51800L,
    val highScoreW3: Long = 42900L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RunRecordDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val runnerName: String = "Explorer Leo",
    val worldId: Long = 0L,
    val characterId: String = "leo",
    val score: Long = 0L,
    val distanceMeters: Long = 0L,
    val coinsCollected: Long = 0L,
    val rubiesCollected: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
