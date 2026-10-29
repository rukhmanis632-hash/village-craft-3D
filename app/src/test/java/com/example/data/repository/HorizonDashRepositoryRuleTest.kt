package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.PlayerProfileDoc
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HorizonDashRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndGetPlayerProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = HorizonDashRepository(firestore)

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePlayerProfile(
                PlayerProfileDoc(
                    userId = uid,
                    displayName = "Explorer Leo",
                    coins = 28500L,
                    rubies = 150L
                )
            )
        }
        assertTrue(saveRes.isSuccess)

        val getRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getPlayerProfile(uid)
        }
        assertTrue(getRes.isSuccess)
        val loaded = getRes.getOrThrow()
        assertNotNull(loaded)
        assertEquals(28500L, loaded?.coins)
    }

    @Test
    fun createAndObserveRunRecords_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = HorizonDashRepository(firestore)

        val recordId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createRunRecord(
                runnerName = "Explorer Leo",
                worldId = 0,
                characterId = "leo",
                score = 42000L,
                distanceMeters = 1850L,
                coinsCollected = 210L,
                rubiesCollected = 3L
            ).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserRunRecords(uid).first { list -> list.any { it.id == recordId } }
        }
        assertTrue(emitted.any { it.id == recordId && it.score == 42000L })
    }

    @Test
    fun getRunRecordById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = HorizonDashRepository(firestore)
        val recordId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createRunRecord(
                runnerName = "Explorer Leo",
                worldId = 1,
                characterId = "kai",
                score = 55000L,
                distanceMeters = 2400L,
                coinsCollected = 320L,
                rubiesCollected = 4L
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = HorizonDashRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getRunRecordById(recordId).getOrThrow()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeUserRunRecords_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = HorizonDashRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserRunRecords(null).first()
            }
            fail("Expected PERMISSION_DENIED exception for unauthenticated user")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@horizondash.test"
        const val BOB_EMAIL = "bob@horizondash.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
