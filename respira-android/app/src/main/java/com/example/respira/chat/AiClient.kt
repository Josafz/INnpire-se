package com.example.respira.chat

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.respira.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Fala com o proxy da IA. Usa só classes do próprio Android (sem biblioteca extra). */
object AiClient {

    class AiException(message: String) : Exception(message)

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Envia o histórico recente (do mais antigo para o mais novo) e devolve a resposta da IA. */
    suspend fun reply(history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        if (!AiConfig.isConfigured) throw AiException("IA não configurada")

        val messages = JSONArray()
        history.forEach { m ->
            val role = if (m.role == ChatMessage.ROLE_USER) "user" else "assistant"
            messages.put(JSONObject().put("role", role).put("content", m.text))
        }
        val body = JSONObject().put("messages", messages).toString()

        val conn = URL(AiConfig.PROXY_URL.trimEnd('/') + "/chat").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8_000
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code !in 200..299) throw AiException("Servidor respondeu $code")

            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(text).getString("reply")
        } catch (e: AiException) {
            throw e
        } catch (e: IOException) {
            throw AiException("Sem conexão com o servidor")
        } catch (e: JSONException) {
            throw AiException("Resposta inválida do servidor")
        } finally {
            conn.disconnect()
        }
    }
}
