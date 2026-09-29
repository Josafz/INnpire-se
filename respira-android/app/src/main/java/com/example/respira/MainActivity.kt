package com.example.respira

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.respira.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        /** A notificação do lembrete manda este extra para abrir direto a respiração guiada. */
        const val EXTRA_OPEN_SOS = "open_sos"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()

            // Veio do toque na notificação do lembrete: já abre a respiração
            if (intent.getBooleanExtra(EXTRA_OPEN_SOS, false)) {
                startActivity(Intent(this, SosActivity::class.java))
            }
        } else {
            updateFab(binding.bottomNav.selectedItemId)
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_chat -> ChatFragment()
                R.id.nav_tips -> TipsFragment()
                R.id.nav_history -> HistoryFragment()
                else -> HomeFragment()
            }
            updateFab(item.itemId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            true
        }

        binding.fabSos.setOnClickListener {
            startActivity(Intent(this, SosActivity::class.java))
        }
    }

    /** Na aba Conversa o botão flutuante cobriria os botões de resposta; ali o SOS fica no topo da tela. */
    private fun updateFab(itemId: Int) {
        binding.fabSos.visibility = if (itemId == R.id.nav_chat) View.GONE else View.VISIBLE
    }
}
