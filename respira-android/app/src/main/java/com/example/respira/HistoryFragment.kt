package com.example.respira

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.respira.adapter.HistoryAdapter
import com.example.respira.data.AppDatabase
import com.example.respira.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val adapter = HistoryAdapter(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
        binding.btnClear.setOnClickListener { confirmClear() }
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val dao = AppDatabase.getInstance(requireContext()).moodDao()
        lifecycleScope.launch {
            val all = dao.getAll()
            adapter.submitList(all)

            binding.tvEmpty.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
            binding.rvHistory.visibility = if (all.isEmpty()) View.GONE else View.VISIBLE

            val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            val last7 = all.filter { it.timestamp >= since }
            binding.tvCountBem.text = "😃\n${last7.count { it.mood == "bem" }}"
            binding.tvCountAnsiedade.text = "😐\n${last7.count { it.mood == "ansiedade" }}"
            binding.tvCountPanico.text = "😰\n${last7.count { it.mood == "panico" }}"
        }
    }

    private fun confirmClear() {
        val dao = AppDatabase.getInstance(requireContext()).moodDao()
        AlertDialog.Builder(requireContext())
            .setTitle("Apagar histórico")
            .setMessage("Isso vai apagar todos os registros. Não pode ser desfeito.")
            .setPositiveButton("Apagar") { _, _ ->
                lifecycleScope.launch {
                    dao.deleteAll()
                    loadHistory()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
