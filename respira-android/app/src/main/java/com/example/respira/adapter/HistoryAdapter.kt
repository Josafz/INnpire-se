package com.example.respira.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.respira.data.MoodEntry
import com.example.respira.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private var items: List<MoodEntry>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val fmt = SimpleDateFormat("dd/MM  HH:mm", Locale("pt", "BR"))

    fun submitList(newItems: List<MoodEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val (emoji, label) = when (item.mood) {
            "bem" -> "😃" to "Bem"
            "ansiedade" -> "😐" to "Ansiedade"
            else -> "😰" to "Pânico"
        }
        holder.binding.tvItemEmoji.text = emoji
        holder.binding.tvItemMood.text = label
        holder.binding.tvItemTime.text = fmt.format(Date(item.timestamp))
    }

    override fun getItemCount() = items.size
}
