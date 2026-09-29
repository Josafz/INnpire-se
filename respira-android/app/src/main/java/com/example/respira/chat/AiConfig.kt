package com.example.respira.chat

/**
 * Onde fica o SEU servidor intermediário (proxy) da IA. Veja a pasta proxy/ do projeto.
 *
 * A chave da API NUNCA fica dentro do app: qualquer pessoa consegue abrir um APK e ler o que
 * está nele. O app só conversa com o proxy, e o proxy (com a chave guardada) fala com a IA.
 *
 * Vazio = IA desligada: a chave "IA online" some e o app funciona só no modo guiado.
 * Emulador do Android falando com o PC: "http://10.0.2.2:8000"
 */
object AiConfig {
    const val PROXY_URL = ""

    val isConfigured: Boolean get() = PROXY_URL.isNotBlank()
}
