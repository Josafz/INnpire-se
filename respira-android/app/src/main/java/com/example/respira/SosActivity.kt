package com.example.respira

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.respira.databinding.ActivitySosBinding
import kotlin.math.PI
import kotlin.math.cos

/**
 * Respiração guiada: 4s inspirando, 6s expirando, em loop, com um círculo
 * que cresce e diminui e frases calmas trocando a cada ciclo.
 */
class SosActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySosBinding
    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var lastCycle = -1
    private var mediaPlayer: MediaPlayer? = null
    private var soundOn = true

    private val messages = listOf(
        "Solte os ombros.",
        "Sinta os pés firmes no chão.",
        "Você está seguro agora.",
        "Isso vai passar.",
        "Deixe o maxilar relaxar.",
        "Você está indo bem."
    )

    companion object {
        private const val IN_MS = 4000L
        private const val CYCLE_MS = 10000L
        private const val MIN_SCALE = 0.6f
    }

    private val tick = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - startTime
            val t = elapsed % CYCLE_MS
            val cycle = (elapsed / CYCLE_MS).toInt()

            val phase: String
            val remain: Long
            val scale: Float

            if (t < IN_MS) {
                phase = "Inspire"
                remain = (IN_MS - t) / 1000 + 1
                scale = MIN_SCALE + (1 - MIN_SCALE) * ease(t.toFloat() / IN_MS)
            } else {
                phase = "Expire"
                remain = (CYCLE_MS - t) / 1000 + 1
                scale = 1f - (1 - MIN_SCALE) * ease((t - IN_MS).toFloat() / (CYCLE_MS - IN_MS))
            }

            binding.viewOrb.scaleX = scale
            binding.viewOrb.scaleY = scale
            binding.tvPhase.text = phase
            binding.tvCount.text = remain.toString()

            if (cycle != lastCycle && cycle > 0) {
                binding.tvMessage.text = messages[(cycle - 1) % messages.size]
                lastCycle = cycle
            }

            handler.postDelayed(this, 50)
        }
    }

    private fun ease(x: Float): Float = (0.5 - cos(PI * x) / 2).toFloat()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvMessage.text = "Acompanhe o círculo. Inspire pelo nariz e expire devagar pela boca."
        binding.btnClose.setOnClickListener { finish() }
        binding.btnBetter.setOnClickListener { finish() }
        binding.btnSound.setOnClickListener { toggleSound() }
    }

    override fun onResume() {
        super.onResume()
        startTime = System.currentTimeMillis()
        lastCycle = -1
        handler.post(tick)
        if (soundOn) startSound()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun toggleSound() {
        soundOn = !soundOn
        binding.btnSound.text = if (soundOn) "🔊 Som ligado" else "🔇 Som desligado"
        if (soundOn) startSound() else mediaPlayer?.pause()
    }

    private fun startSound() {
        try {
            if (mediaPlayer == null) {
                // Procura app/src/main/res/raw/calm_loop.mp3. Se não existir,
                // resId fica 0 e o app segue normalmente, sem som.
                val resId = resources.getIdentifier("calm_loop", "raw", packageName)
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(this, resId)
                    mediaPlayer?.isLooping = true
                }
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Sem áudio disponível: o app continua funcionando normalmente
        }
    }
}
