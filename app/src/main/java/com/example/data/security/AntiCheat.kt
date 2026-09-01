package com.example.data.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.max

object AntiCheat {
    const val DEFAULT_HMAC_SECRET = "super_secret_rock_key_2026"
    const val MAX_HUMAN_CPS = 25
    const val MIN_TAP_INTERVAL_MS = 40L // 1000 / 25 = 40ms

    fun signBatch(
        userId: String,
        timestamp: Long,
        clicks: Int,
        secret: String = DEFAULT_HMAC_SECRET
    ): String {
        val payload = "$userId:$timestamp:$clicks"
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(keySpec)
            val hmacBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            hmacBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback SHA-256 if Mac fails
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest("$payload:$secret".toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        }
    }

    fun isBatchValid(
        batchClicks: Int,
        durationSeconds: Int
    ): Pair<Boolean, String?> {
        if (batchClicks <= 0) {
            return Pair(false, "Geçersiz tıklama sayısı")
        }
        val effectiveDuration = max(durationSeconds, 1)
        val maxAllowed = effectiveDuration * MAX_HUMAN_CPS
        if (batchClicks > maxAllowed) {
            return Pair(false, "Hız sınırı aşıldı (Maks: $maxAllowed tık / ${effectiveDuration}sn)")
        }
        return Pair(true, null)
    }
}

class TapRateLimiter(
    private val minIntervalMs: Long = AntiCheat.MIN_TAP_INTERVAL_MS,
    private val maxCps: Int = AntiCheat.MAX_HUMAN_CPS
) {
    private var lastTapTimestamp = 0L
    private val tapTimestamps = ArrayDeque<Long>()

    /**
     * Returns true if tap is accepted, false if rate limited.
     */
    @Synchronized
    fun tryTap(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastTapTimestamp < minIntervalMs) {
            return false
        }

        // Clean timestamps older than 1 second
        val cutoff = now - 1000L
        while (tapTimestamps.isNotEmpty() && tapTimestamps.first() < cutoff) {
            tapTimestamps.removeFirst()
        }

        if (tapTimestamps.size >= maxCps) {
            return false
        }

        lastTapTimestamp = now
        tapTimestamps.addLast(now)
        return true
    }

    /**
     * Calculates current CPS in a 1-second sliding window.
     */
    @Synchronized
    fun getCurrentCps(): Int {
        val now = System.currentTimeMillis()
        val cutoff = now - 1000L
        while (tapTimestamps.isNotEmpty() && tapTimestamps.first() < cutoff) {
            tapTimestamps.removeFirst()
        }
        return tapTimestamps.size
    }
}
