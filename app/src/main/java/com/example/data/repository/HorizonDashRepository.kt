package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.PlayerProfileDoc
import com.example.data.RunRecordDoc
import com.example.data.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class HorizonDashRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observePlayerProfile(userId: String = requireUserId()): Flow<PlayerProfileDoc?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PlayerProfileDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    suspend fun getPlayerProfile(userId: String = requireUserId()): Result<PlayerProfileDoc?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            val doc = if (snap.exists()) {
                snap.toObject(
                    PlayerProfileDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun savePlayerProfile(profile: PlayerProfileDoc): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingCreatedAt = existingSnap.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val updatedDoc = profile.copy(
                    id = uid,
                    userId = uid,
                    createdAt = existingCreatedAt,
                    updatedAt = null
                )
                docRef.set(updatedDoc).await()
            } else {
                val newDoc = profile.copy(
                    id = uid,
                    userId = uid,
                    createdAt = null,
                    updatedAt = null
                )
                docRef.set(newDoc).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createRunRecord(
        runnerName: String,
        worldId: Int,
        characterId: String,
        score: Long,
        distanceMeters: Long,
        coinsCollected: Long,
        rubiesCollected: Long
    ): Result<String> {
        val uid = requireUserId()
        val recordId = "run_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("run_records").document(recordId)
        val record = RunRecordDoc(
            id = recordId,
            userId = uid,
            runnerName = runnerName.take(50).ifBlank { "Explorer" },
            worldId = worldId.coerceIn(0, 3).toLong(),
            characterId = characterId.take(32).ifBlank { "leo" },
            score = score.coerceAtLeast(0L),
            distanceMeters = distanceMeters.coerceAtLeast(0L),
            coinsCollected = coinsCollected.coerceAtLeast(0L),
            rubiesCollected = rubiesCollected.coerceAtLeast(0L),
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(record).await()
            Result.success(recordId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserRunRecords(userId: String = requireUserId()): Result<List<RunRecordDoc>> {
        val path = "run_records"
        return try {
            val snap = db.collection(path)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val list = snap.toObjects(
                RunRecordDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getRunRecordById(recordId: String): Result<RunRecordDoc?> {
        val docRef = db.collection("run_records").document(recordId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    RunRecordDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserRunRecords(userId: String? = auth.currentUser?.uid): Flow<List<RunRecordDoc>> =
        db.collection("run_records")
            .let { col -> if (userId != null) col.whereEqualTo("userId", userId) else col }
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RunRecordDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "run_records")
                throw error
            }
}
