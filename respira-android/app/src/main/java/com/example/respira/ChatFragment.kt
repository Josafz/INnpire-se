package com.example.respira

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.respira.adapter.ChatAdapter
import com.example.respira.chat.AiClient
import com.example.respira.chat.AiConfig
import com.example.respira.chat.ChatAction
import com.example.respira.chat.ChatOption
import com.example.respira.chat.ChatTree
import com.example.respira.chat.CrisisDetector
import com.example.respira.data.AppDatabase
import com.example.respira.data.ChatMessage
import com.example.respira.data.MoodEntry
import com.example.respira.databinding.FragmentChatBinding
import com.example.respira.util.Dialer
import com.example.respira.util.Prefs
import kotlinx.coroutines.launch

/**
 * Chat HÍBRIDO:
 *  - Modo guiado (padrão): botões de resposta rápida, 100% offline, sem digitar.
 *  - Modo IA (opcional): caixa de texto; só liga com internet, com o aceite do usuário
 *    e com o proxy configurado (AiConfig). Texto de crise NUNCA vai para a IA.
 */
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val adapter = ChatAdapter()
    private var busy = false            // evita toque duplo enquanto uma resposta está em andamento
    private var updatingSwitch = false  // evita loop quando a chave é mudada por código
    private var aiMode = false

    private val chatDao get() = AppDatabase.getInstance(requireContext()).chatDao()
    private val moodDao get() = AppDatabase.getInstance(requireContext()).moodDao()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChat.adapter = adapter

        binding.btnChatSos.setOnClickListener {
            startActivity(Intent(requireContext(), SosActivity::class.java))
        }
        binding.btnClearChat.setOnClickListener { confirmClear() }
        binding.btnSend.setOnClickListener { sendFreeText() }

        // Sem proxy configurado, a IA nem aparece: o app segue só no modo guiado
        binding.swAi.visibility = if (AiConfig.isConfigured) View.VISIBLE else View.GONE
        binding.swAi.setOnCheckedChangeListener { _, checked ->
            if (updatingSwitch) return@setOnCheckedChangeListener
            if (checked) tryEnableAi() else setAiMode(false)
        }

        loadConversation()
    }

    // ------------------------------------------------------------------ conversa

    private fun loadConversation() {
        viewLifecycleOwner.lifecycleScope.launch {
            val saved = chatDao.getAll()
            adapter.setAll(saved)
            scrollToEnd()

            val ctx = requireContext()
            aiMode = AiConfig.isConfigured && Prefs.aiConsent(ctx) && Prefs.aiEnabled(ctx)
            setSwitch(aiMode)
            applyAiUi()

            if (saved.isEmpty()) {
                playNode(ChatTree.ROOT)
            } else if (!aiMode) {
                renderOptions(ChatTree.node(Prefs.chatNode(ctx)).options)
            }
        }
    }

    /** O assistente "fala" as mensagens do nó, guarda o ponto da conversa e mostra os botões. */
    private suspend fun playNode(id: String) {
        val node = ChatTree.node(id)
        Prefs.setChatNode(requireContext(), node.id)
        node.bot.forEach { addMessage(ChatMessage.ROLE_BOT, it) }
        renderOptions(node.options)
    }

    private suspend fun addMessage(role: String, text: String) {
        val message = ChatMessage(role = role, text = text, timestamp = System.currentTimeMillis())
        val id = chatDao.insert(message)
        adapter.add(message.copy(id = id))
        scrollToEnd()
    }

    private fun scrollToEnd() {
        if (adapter.itemCount > 0) binding.rvChat.scrollToPosition(adapter.itemCount - 1)
    }

    // ------------------------------------------------------------------ modo guiado

    private fun renderOptions(options: List<ChatOption>) {
        binding.llOptions.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        options.forEach { option ->
            val button = inflater.inflate(R.layout.item_chat_option, binding.llOptions, false) as Button
            button.text = option.label
            button.setOnClickListener { onOptionClicked(option) }
            binding.llOptions.addView(button)
        }
    }

    private fun onOptionClicked(option: ChatOption) {
        if (busy) return
        busy = true
        binding.llOptions.removeAllViews()   // some os botões: impede toque duplo
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                addMessage(ChatMessage.ROLE_USER, option.label)
                runAction(option.action)
                playNode(option.next)
            } finally {
                busy = false
            }
        }
    }

    private suspend fun runAction(action: ChatAction) {
        val ctx = requireContext()
        when (action) {
            ChatAction.NONE -> Unit
            ChatAction.OPEN_SOS -> startActivity(Intent(ctx, SosActivity::class.java))
            ChatAction.CALL_CVV -> Dialer.dial(ctx, "188")
            ChatAction.CALL_SAMU -> Dialer.dial(ctx, "192")
            ChatAction.LOG_BEM -> logMood("bem")
            ChatAction.LOG_ANSIEDADE -> logMood("ansiedade")
            ChatAction.LOG_PANICO -> logMood("panico")
        }
    }

    /** O chat grava na MESMA tabela do registro de humor da tela inicial (aparece no Histórico). */
    private suspend fun logMood(mood: String) {
        moodDao.insert(MoodEntry(mood = mood, timestamp = System.currentTimeMillis()))
    }

    // ------------------------------------------------------------------ modo IA

    private fun sendFreeText() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) {
            binding.etMessage.error = "Escreva algo primeiro"
            return
        }
        if (busy) return
        busy = true
        binding.etMessage.text.clear()
        binding.llOptions.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                addMessage(ChatMessage.ROLE_USER, text)
                if (CrisisDetector.isCrisis(text)) {
                    playNode(ChatTree.CRISIS)   // resposta fixa com CVV/SAMU; NÃO envia à IA
                    return@launch
                }
                askAi()
            } finally {
                busy = false
            }
        }
    }

    private suspend fun askAi() {
        if (!AiClient.isOnline(requireContext())) {
            addMessage(ChatMessage.ROLE_BOT, "Estou sem internet agora. Voltei para o modo guiado.")
            setAiMode(false)
            return
        }
        binding.tvTyping.visibility = View.VISIBLE
        try {
            val history = chatDao.getLast(12).reversed()   // getLast vem do mais novo; a IA quer do mais antigo
            addMessage(ChatMessage.ROLE_BOT, AiClient.reply(history))
        } catch (e: AiClient.AiException) {
            addMessage(ChatMessage.ROLE_BOT, "Não consegui falar com o servidor agora. Voltei para o modo guiado.")
            setAiMode(false)
        } finally {
            _binding?.tvTyping?.visibility = View.GONE   // a tela pode já ter sido fechada
        }
    }

    private fun tryEnableAi() {
        val ctx = requireContext()
        if (!AiClient.isOnline(ctx)) {
            Toast.makeText(ctx, "Sem internet: a IA precisa de conexão.", Toast.LENGTH_LONG).show()
            setSwitch(false)
            return
        }
        if (Prefs.aiConsent(ctx)) {
            setAiMode(true)
            return
        }
        AlertDialog.Builder(ctx)
            .setTitle("Usar IA online?")
            .setMessage(
                "Com a IA ligada, o que você escrever (e as últimas mensagens da conversa) é enviado " +
                    "a um servidor para gerar a resposta. Não escreva dados pessoais, como nome completo " +
                    "ou documentos. A IA não substitui um profissional. Em crise, ligue 188 (CVV). " +
                    "Você pode desligar quando quiser."
            )
            .setPositiveButton("Aceito") { _, _ ->
                Prefs.setAiConsent(ctx, true)
                setAiMode(true)
            }
            .setNegativeButton("Agora não") { _, _ -> setSwitch(false) }
            .setOnCancelListener { setSwitch(false) }
            .show()
    }

    private fun setAiMode(on: Boolean) {
        aiMode = on
        Prefs.setAiEnabled(requireContext(), on)
        setSwitch(on)
        applyAiUi()
        if (on) {
            binding.llOptions.removeAllViews()
        } else {
            renderOptions(ChatTree.node(Prefs.chatNode(requireContext())).options)
        }
    }

    private fun applyAiUi() {
        binding.llInput.visibility = if (aiMode) View.VISIBLE else View.GONE
        binding.tvAiStatus.visibility = if (aiMode) View.VISIBLE else View.GONE
    }

    private fun setSwitch(checked: Boolean) {
        updatingSwitch = true
        binding.swAi.isChecked = checked
        updatingSwitch = false
    }

    // ------------------------------------------------------------------ limpar

    private fun confirmClear() {
        AlertDialog.Builder(requireContext())
            .setTitle("Limpar conversa")
            .setMessage("Isso apaga todas as mensagens desta conversa. Não pode ser desfeito.")
            .setPositiveButton("Limpar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    chatDao.deleteAll()
                    adapter.setAll(emptyList())
                    playNode(ChatTree.ROOT)
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
