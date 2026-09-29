package com.example.respira.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.respira.R
import com.example.respira.data.ChatMessage
import com.example.respira.databinding.ItemChatMessageBinding

/** Desenha os balões da conversa: os do assistente à esquerda e os do usuário à direita. */
class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val items = mutableListOf<ChatMessage>()

    fun setAll(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun add(message: ChatMessage) {
        items.add(message)
        notifyItemInserted(items.size - 1)   // anima só a linha nova, sem redesenhar a lista toda
    }

    inner class ViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = items[position]
        val isUser = message.role == ChatMessage.ROLE_USER

        holder.binding.tvBubble.text = message.text
        holder.binding.tvBubble.setBackgroundResource(
            if (isUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_bot
        )
        holder.binding.rootRow.gravity = if (isUser) Gravity.END else Gravity.START
    }

    override fun getItemCount() = items.size
}
