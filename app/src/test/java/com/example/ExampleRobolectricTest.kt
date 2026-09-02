package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.RockRepository
import com.example.data.security.AntiCheat
import com.example.data.security.TapRateLimiter
import com.example.data.sync.RockCloudSync
import com.example.data.sync.RegistrationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Sanal Taş", appName)
  }

  @Test
  fun `anti-cheat HMAC signature generation and validation`() {
    val userId = "usr_test_1234"
    val timestamp = 1700000000000L
    val clicks = 25
    val signature = AntiCheat.signBatch(userId, timestamp, clicks)
    
    assertNotNull(signature)
    assertTrue(signature.length >= 32)

    val (isValid, _) = AntiCheat.isBatchValid(batchClicks = 50, durationSeconds = 5)
    assertTrue(isValid)

    val (isOverLimit, errorReason) = AntiCheat.isBatchValid(batchClicks = 300, durationSeconds = 5)
    assertFalse(isOverLimit)
    assertNotNull(errorReason)
  }

  @Test
  fun `tap rate limiter enforces maximum 25 cps threshold`() {
    val limiter = TapRateLimiter(minIntervalMs = 40, maxCps = 25)
    
    val firstTap = limiter.tryTap()
    assertTrue(firstTap)

    // Immediate second tap within 40ms should be rejected
    val immediateTap = limiter.tryTap()
    assertFalse(immediateTap)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `unique username registration and rejection of duplicates`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testScope = TestScope()
    val cloudSync = RockCloudSync(context, testScope)

    // First registration of user
    val res1 = cloudSync.registerUsername("KayaUstasi", "usr_1001", "TR")
    assertTrue(res1 is RegistrationResult.Success)

    // Second registration with the exact same name by another user should be rejected
    val res2 = cloudSync.registerUsername("KayaUstasi", "usr_1002", "DE")
    assertTrue(res2 is RegistrationResult.Error)
    assertEquals("Bu kullanıcı adı başka bir oyuncu tarafından alınmış. Lütfen farklı bir isim seçin.", (res2 as RegistrationResult.Error).message)

    // Short name should be rejected
    val resShort = cloudSync.registerUsername("ab", "usr_1003", "TR")
    assertTrue(resShort is RegistrationResult.Error)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `local clicks incrementation and unsynced tracking`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testScope = TestScope()
    val repo = RockRepository(context, testScope)

    delay(200)

    repeat(50) {
      repo.incrementClick()
    }

    val stats = repo.getLocalStatsSnapshot()
    assertTrue(stats.totalClicks >= 50L)
  }
}
