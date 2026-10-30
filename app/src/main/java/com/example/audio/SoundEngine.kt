package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050
    private var bgmJob: Job? = null

    var soundEnabled: Boolean = true
    var bgmVolume: Float = 0.65f
    var sfxVolume: Float = 0.85f
    var footstepVolume: Float = 0.75f

    private var stepToggle = 0
    private var currentWorldId = 0
    private var isBgmRunning = false

    fun playFootstep(worldId: Int) {
        if (!soundEnabled || footstepVolume <= 0.01f) return
        stepToggle = 1 - stepToggle
        val vol = footstepVolume * 0.7f
        scope.launch {
            val dur = 0.12f
            val samples = ShortArray((sampleRate * dur).toInt())
            val bodyFreq = when (worldId) {
                0 -> if (stepToggle == 0) 96f else 110f // Ancient stone flagstones
                1 -> if (stepToggle == 0) 145f else 162f // Cyber metal grates
                2 -> if (stepToggle == 0) 82f else 92f // Volcanic basalt rock
                else -> if (stepToggle == 0) 118f else 132f // Wooden bridge & snow
            }
            val woodRes = when (worldId) {
                1 -> 540f
                2 -> 185f
                3 -> 310f
                else -> 0f
            }
            var lp = 0f
            for (i in samples.indices) {
                val t = i.toFloat() / sampleRate
                val heelEnv = exp(-t * 48f)
                val gritEnv = exp(-max(0f, t - 0.015f) * 34f)
                val noise = Random.nextFloat() * 2f - 1f
                lp += 0.3f * (noise - lp)
                val thud = sin(2f * PI.toFloat() * bodyFreq * exp(-t * 16f) * t) * heelEnv * 0.7f
                val res = if (woodRes > 0f) sin(2f * PI.toFloat() * woodRes * t) * exp(-t * 36f) * 0.3f else 0f
                val v = (thud + res + lp * gritEnv * 0.45f) * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playSwipeWhoosh() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.65f
        scope.launch {
            val dur = 0.16f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            var lp1 = 0f
            var lp2 = 0f
            for (i in 0 until len) {
                val norm = i.toFloat() / len
                val env = sin(PI.toFloat() * norm) * exp(-norm * 1.3f)
                val f = 0.08f + 0.28f * norm
                val n = Random.nextFloat() * 2f - 1f
                lp1 += f * (n - lp1)
                lp2 += f * (lp1 - lp2)
                val v = (lp1 - lp2) * 2.2f * env * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playJumpWhoosh() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.75f
        scope.launch {
            val dur = 0.22f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            var lp = 0f
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val norm = i.toFloat() / len
                val env = sin(PI.toFloat() * norm)
                val sweep = 180f + 340f * norm
                lp += 0.2f * ((Random.nextFloat() * 2f - 1f) - lp)
                val tone = sin(2f * PI.toFloat() * sweep * t) * 0.35f
                val v = (tone + lp * 0.55f) * env * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playSlideScrape() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.7f
        scope.launch {
            val dur = 0.26f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            var lp = 0f
            for (i in 0 until len) {
                val norm = i.toFloat() / len
                val env = sin(PI.toFloat() * norm) * exp(-norm * 1.1f)
                val n = Random.nextFloat() * 2f - 1f
                lp += (0.28f - 0.16f * norm) * (n - lp)
                val v = lp * 1.4f * env * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playCoinChime() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.7f
        scope.launch {
            val dur = 0.26f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val r1 = sin(2f * PI.toFloat() * 1760f * t) * exp(-t * 18f) * 0.4f
                val r2 = sin(2f * PI.toFloat() * 2637f * t) * exp(-max(0f, t - 0.025f) * 15f) * 0.48f
                val v = (r1 + r2) * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playRubyChime() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.8f
        scope.launch {
            val dur = 0.36f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            val freqs = floatArrayOf(1174.66f, 1479.98f, 1760f, 2349.32f)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                var s = 0f
                for (idx in freqs.indices) {
                    val delay = idx * 0.028f
                    if (t >= delay) {
                        val dt = t - delay
                        s += sin(2f * PI.toFloat() * freqs[idx] * dt) * exp(-dt * 12f) * 0.25f
                    }
                }
                samples[i] = ((s * vol).coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playPowerupSurge() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.8f
        scope.launch {
            val dur = 0.42f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            val chord = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val env = sin(PI.toFloat() * (t / dur))
                var tones = 0f
                for (idx in chord.indices) {
                    tones += sin(2f * PI.toFloat() * chord[idx] * t) * exp(-t * (4f + idx)) * 0.22f
                }
                val v = tones * env * 1.3f * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playImpact(isShatter: Boolean) {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.85f
        scope.launch {
            val dur = if (isShatter) 0.32f else 0.44f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            var lp = 0f
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val baseFreq = if (isShatter) 150f else 85f
                val punch = sin(2f * PI.toFloat() * baseFreq * exp(-t * 14f) * t) * exp(-t * 11f) * 0.65f
                val n = Random.nextFloat() * 2f - 1f
                lp += (if (isShatter) 0.55f else 0.25f) * (n - lp)
                val debris = lp * exp(-t * 8f) * 0.55f
                val v = (punch + debris) * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playUiClick() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.55f
        scope.launch {
            val dur = 0.055f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val env = exp(-t * 85f)
                val v = sin(2f * PI.toFloat() * 640f * exp(-t * 24f) * t) * env * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playNearMissSpark() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.8f
        scope.launch {
            val dur = 0.28f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            var lp = 0f
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val norm = i.toFloat() / len
                // Sub-drop time-dilation thump + electric spark sweep
                val subDrop = sin(2f * PI.toFloat() * (145f * exp(-t * 14f) + 55f) * t) * exp(-t * 10f) * 0.65f
                val sparkFreq = 920f + 1680f * norm
                val spark = sin(2f * PI.toFloat() * sparkFreq * t) * exp(-t * 16f) * 0.45f
                lp += 0.45f * ((Random.nextFloat() * 2f - 1f) - lp)
                val crackle = lp * exp(-t * 22f) * 0.35f
                val v = (subDrop + spark + crackle) * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playHyperRunDrop() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.85f
        scope.launch {
            val dur = 0.48f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            val chord = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val norm = i.toFloat() / len
                val env = sin(PI.toFloat() * min(1f, norm * 1.2f)) * exp(-t * 3.8f)
                var synth = 0f
                for (f in chord) {
                    synth += sin(2f * PI.toFloat() * (f * (0.85f + 0.15f * min(1f, norm * 3f))) * t)
                }
                val bass = sin(2f * PI.toFloat() * 130.81f * t) * exp(-t * 5f) * 0.55f
                val v = (synth * 0.24f * env + bass) * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playFlashMissionComplete() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.85f
        scope.launch {
            val dur = 0.42f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            val notes = floatArrayOf(587.33f, 783.99f, 987.77f, 1174.66f)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val noteIdx = ((i * notes.size) / len).coerceIn(0, notes.lastIndex)
                val freq = notes[noteIdx]
                val localT = (t * notes.size) % (dur / notes.size)
                val env = exp(-localT * 18f)
                val tone = (sin(2f * PI.toFloat() * freq * t) + 0.45f * sin(4f * PI.toFloat() * freq * t)) * env * 0.65f
                samples[i] = ((tone * vol).coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playTitanRoar() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.92f
        scope.launch {
            val dur = 0.68f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            var lp = 0f
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val norm = i.toFloat() / len
                val env = sin(PI.toFloat() * min(1f, norm * 1.15f)) * exp(-t * 2.1f)
                // Guttural monster sub-growl + formants + roar distortion
                val growlFreq = 92f - 34f * norm + sin(2f * PI.toFloat() * 19f * t) * 11f
                val sub = sin(2f * PI.toFloat() * growlFreq * t) * 0.58f
                val formant = sin(2f * PI.toFloat() * (growlFreq * 2.7f) * t) * 0.34f
                val n = Random.nextFloat() * 2f - 1f
                lp += 0.32f * (n - lp)
                val breath = lp * 0.55f
                val v = (sub + formant + breath) * env * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playPortalWarp() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.85f
        scope.launch {
            val dur = 0.55f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val norm = i.toFloat() / len
                val env = sin(PI.toFloat() * norm)
                val sweep1 = 220f + 660f * norm * norm
                val sweep2 = 330f + 990f * norm
                val shimmer = sin(2f * PI.toFloat() * sweep1 * t) * 0.45f +
                    sin(2f * PI.toFloat() * sweep2 * t + sin(t * 45f) * 2.5f) * 0.40f
                val v = shimmer * env * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playRadioCommsChime() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.65f
        scope.launch {
            val dur = 0.18f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val freq = if (t < 0.08f) 1320f else 1760f
                val env = exp(-(t % 0.09f) * 26f)
                val staticNoise = (Random.nextFloat() * 2f - 1f) * 0.08f
                val v = (sin(2f * PI.toFloat() * freq * t) * 0.55f * env + staticNoise) * vol
                samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playRelicGrab() {
        if (!soundEnabled || sfxVolume <= 0.01f) return
        val vol = sfxVolume * 0.85f
        scope.launch {
            val dur = 0.46f
            val len = (sampleRate * dur).toInt()
            val samples = ShortArray(len)
            val freqs = floatArrayOf(523.25f, 783.99f, 1046.5f, 1567.98f)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                var sig = 0f
                for (f in freqs) {
                    sig += sin(2f * PI.toFloat() * f * t) * exp(-t * 6.5f) * 0.25f
                }
                samples[i] = ((sig * vol).coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun startWorldBgm(worldId: Int) {
        currentWorldId = worldId
        if (!soundEnabled) {
            stopBgm()
            return
        }
        if (isBgmRunning && bgmJob?.isActive == true) return
        isBgmRunning = true
        bgmJob?.cancel()
        bgmJob = scope.launch {
            val scales = arrayOf(
                floatArrayOf(293.66f, 349.23f, 440.0f, 523.25f), // Ruins Tribal D minor pentatonic
                floatArrayOf(261.63f, 311.13f, 392.0f, 466.16f), // Cyber City C minor synth
                floatArrayOf(246.94f, 293.66f, 369.99f, 329.63f), // Lava Cave B phrygian war drums
                floatArrayOf(329.63f, 392.0f, 493.88f, 587.33f)  // Snowy Peak E minor alpine flute
            )
            var stepIndex = 0
            while (isActive && isBgmRunning) {
                if (!soundEnabled || bgmVolume <= 0.02f) {
                    delay(250)
                    continue
                }
                val w = currentWorldId.coerceIn(0, 3)
                val scale = scales[w]
                val beatDur = 0.22f
                val len = (sampleRate * beatDur).toInt()
                val samples = ShortArray(len)
                val isBassHit = stepIndex % 4 == 0
                val isToneHit = stepIndex % 4 == 2
                val fluteFreq = scale[(stepIndex / 4) % scale.size]
                val vol = bgmVolume * 0.26f

                for (i in 0 until len) {
                    val t = i.toFloat() / sampleRate
                    val drum = when {
                        isBassHit -> sin(2f * PI.toFloat() * (76f * exp(-t * 16f) + 52f) * t) * exp(-t * 13f) * 0.65f
                        isToneHit -> sin(2f * PI.toFloat() * (162f * exp(-t * 14f) + 122f) * t) * exp(-t * 18f) * 0.45f
                        else -> (Random.nextFloat() * 2f - 1f) * exp(-t * 36f) * 0.18f
                    }
                    val breathEnv = sin(PI.toFloat() * min(1f, t / beatDur)) * 0.25f
                    val flute = sin(2f * PI.toFloat() * fluteFreq * t) * breathEnv
                    val v = (drum + flute) * vol
                    samples[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                playPcmBuffer(samples)
                stepIndex = (stepIndex + 1) % 16
                delay(215)
            }
        }
    }

    fun stopBgm() {
        isBgmRunning = false
        bgmJob?.cancel()
        bgmJob = null
    }

    private fun playPcmBuffer(samples: ShortArray) {
        try {
            val byteCount = samples.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(max(byteCount, 2048))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            scope.launch {
                val ms = (samples.size * 1000L / sampleRate) + 80L
                delay(ms)
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
            // Gracefully ignore in JVM unit test environments without audio hardware
        }
    }
}
