package com.example.respira.chat

/** O que um botão do chat faz, além de levar para a próxima pergunta. */
enum class ChatAction {
    NONE,
    OPEN_SOS,
    CALL_CVV,
    CALL_SAMU,
    LOG_BEM,
    LOG_ANSIEDADE,
    LOG_PANICO
}

/** Um botão de resposta rápida: o texto, para qual nó vai e a ação extra (opcional). */
data class ChatOption(
    val label: String,
    val next: String,
    val action: ChatAction = ChatAction.NONE
)

/** Um "passo" da conversa: o que o assistente fala e quais botões aparecem. */
data class ChatNode(
    val id: String,
    val bot: List<String>,
    val options: List<ChatOption>
)

/**
 * A conversa guiada (modo offline). É uma árvore/grafo de decisão guardada
 * num Map: cada nó tem um id e aponta para outros nós pelo id.
 * Não depende de internet nem de IA, então as respostas são sempre previsíveis.
 */
object ChatTree {

    const val ROOT = "inicio"
    const val CRISIS = "crise"

    private val nodes: Map<String, ChatNode> = listOf(
        ChatNode(
            "inicio",
            listOf("Oi! Sou o assistente do Respira. Como você está agora?"),
            listOf(
                ChatOption("😃 Estou bem", "bem"),
                ChatOption("😐 Estou ansioso(a)", "ansioso"),
                ChatOption("😰 Estou em pânico", "panico"),
                ChatOption("💬 Só quero conversar", "livre"),
                ChatOption("🆘 Preciso de ajuda urgente", "urgente")
            )
        ),
        ChatNode(
            "bem",
            listOf("Que bom saber! 😃", "Quer guardar isso no seu histórico?"),
            listOf(
                ChatOption("Sim, registrar", "bem_ok", ChatAction.LOG_BEM),
                ChatOption("Agora não", "fim")
            )
        ),
        ChatNode(
            "bem_ok",
            listOf("Pronto, registrei como Bem. Continue cuidando de você!"),
            listOf(ChatOption("Terminar", "fim"))
        ),
        ChatNode(
            "ansioso",
            listOf(
                "Sinto muito que você esteja assim. Obrigado por me contar.",
                "A ansiedade costuma passar. Quer tentar algo agora?"
            ),
            listOf(
                ChatOption("Respirar comigo (1 minuto)", "resp", ChatAction.OPEN_SOS),
                ChatOption("Ver dicas rápidas", "ansioso_dicas"),
                ChatOption("Registrar como Ansiedade", "ansioso_reg", ChatAction.LOG_ANSIEDADE),
                ChatOption("Falar com alguém", "urgente")
            )
        ),
        ChatNode(
            "ansioso_dicas",
            listOf(
                "Três coisas rápidas:",
                "1) Beba um copo de água, devagar.",
                "2) Diga em voz baixa 5 coisas que você vê ao redor.",
                "3) Solte os ombros e relaxe o maxilar."
            ),
            listOf(
                ChatOption("Respirar comigo", "resp", ChatAction.OPEN_SOS),
                ChatOption("Registrar como Ansiedade", "ansioso_reg", ChatAction.LOG_ANSIEDADE),
                ChatOption("Voltar ao começo", "inicio")
            )
        ),
        ChatNode(
            "ansioso_reg",
            listOf("Registrado como Ansiedade. Você está fazendo bem em cuidar de si."),
            listOf(
                ChatOption("Respirar comigo", "resp", ChatAction.OPEN_SOS),
                ChatOption("Terminar", "fim")
            )
        ),
        ChatNode(
            "panico",
            listOf(
                "Você não está sozinho(a). Isso vai passar.",
                "Vamos fazer a respiração agora, sem pressa."
            ),
            listOf(
                ChatOption("Abrir respiração guiada", "resp", ChatAction.OPEN_SOS),
                ChatOption("Preciso falar com alguém", "urgente"),
                ChatOption("Registrar como Pânico", "panico_reg", ChatAction.LOG_PANICO)
            )
        ),
        ChatNode(
            "panico_reg",
            listOf("Registrado como Pânico. Respire devagar, eu fico por aqui."),
            listOf(
                ChatOption("Abrir respiração guiada", "resp", ChatAction.OPEN_SOS),
                ChatOption("Terminar", "fim")
            )
        ),
        ChatNode(
            "resp",
            listOf("Quando voltar da respiração, me conta: como você está agora?"),
            listOf(
                ChatOption("😃 Estou melhor", "melhor"),
                ChatOption("😐 Continuo igual", "ansioso"),
                ChatOption("😰 Estou pior", "urgente")
            )
        ),
        ChatNode(
            "melhor",
            listOf("Que bom que ajudou! 😃", "Quer registrar como Bem?"),
            listOf(
                ChatOption("Sim, registrar", "bem_ok", ChatAction.LOG_BEM),
                ChatOption("Agora não", "fim")
            )
        ),
        ChatNode(
            "urgente",
            listOf(
                "Se você precisa conversar agora, ligue:",
                "CVV: 188 (24 horas, gratuito)",
                "Em emergência médica: SAMU 192"
            ),
            listOf(
                ChatOption("Ligar para o CVV (188)", "fim", ChatAction.CALL_CVV),
                ChatOption("Ligar para o SAMU (192)", "fim", ChatAction.CALL_SAMU),
                ChatOption("Abrir respiração guiada", "resp", ChatAction.OPEN_SOS),
                ChatOption("Voltar ao começo", "inicio")
            )
        ),
        ChatNode(
            "livre",
            listOf(
                "Tudo bem. Para conversar livremente, ligue a chave \"IA online\" no alto desta tela (precisa de internet).",
                "Sem a IA, eu te guio por opções."
            ),
            listOf(ChatOption("Voltar ao começo", "inicio"))
        ),
        ChatNode(
            "crise",
            listOf(
                "Sinto muito que você esteja passando por isso. Você não precisa enfrentar isso sozinho(a).",
                "Por favor, fale com alguém agora: CVV 188 (24 horas, gratuito). Em emergência, SAMU 192."
            ),
            listOf(
                ChatOption("Ligar para o CVV (188)", "fim", ChatAction.CALL_CVV),
                ChatOption("Ligar para o SAMU (192)", "fim", ChatAction.CALL_SAMU),
                ChatOption("Respirar comigo", "resp", ChatAction.OPEN_SOS)
            )
        ),
        ChatNode(
            "fim",
            listOf("Estou por aqui quando precisar. 🌿"),
            listOf(ChatOption("Recomeçar", "inicio"))
        )
    ).associateBy { it.id }

    /** Devolve o nó pelo id; se o id não existir, volta para o começo em vez de quebrar. */
    fun node(id: String): ChatNode = nodes[id] ?: nodes.getValue(ROOT)

    fun allNodes(): Collection<ChatNode> = nodes.values
}
