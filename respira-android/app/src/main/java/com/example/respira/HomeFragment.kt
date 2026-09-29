package com.example.respira

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.respira.data.AppDatabase
import com.example.respira.data.MoodEntry
import com.example.respira.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvGreeting.text = greeting()

        binding.btnSosHome.setOnClickListener {
            startActivity(Intent(requireContext(), SosActivity::class.java))
        }

        binding.btnMoodBem.setOnClickListener { registrar("bem") }
        binding.btnMoodAnsiedade.setOnClickListener { registrar("ansiedade") }
        binding.btnMoodPanico.setOnClickListener { registrar("panico") }
    }

    private fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Bom dia"
            hour < 18 -> "Boa tarde"
            else -> "Boa noite"
        }
    }

    private fun registrar(mood: String) {
        val dao = AppDatabase.getInstance(requireContext()).moodDao()
        val now = System.currentTimeMillis()

        lifecycleScope.launch {
            dao.insert(MoodEntry(mood = mood, timestamp = now))

            val fmt = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
            val label = when (mood) {
                "bem" -> "😃 Bem"
                "ansiedade" -> "😐 Ansiedade"
                else -> "😰 Pânico"
            }
            binding.tvNote.visibility = View.VISIBLE
            binding.tvNote.text = "Registrado: $label, hoje às ${fmt.format(Date(now))}."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
