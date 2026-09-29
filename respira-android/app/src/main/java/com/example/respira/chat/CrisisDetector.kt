package com.example.respira.chat

import java.text.Normalizer

/**
 * Rede de segurança do chat: se o texto livre parecer de crise, a mensagem
 * NÃO é enviada à IA e o app mostra o CVV/SAMU na hora.
 *
 * Limite honesto: é só busca por trechos, não entende contexto. Pode deixar passar
 * frases que não previmos e pode disparar por engano ("não aguento mais esse calor").
 * O disparo por engano custa pouco (mostra ajuda); por isso a lista é conservadora.
 */
object CrisisDetector {

    private val patterns = listOf(
        "me matar", "quero morrer", "vou morrer", "suicid",
        "acabar com a minha vida", "acabar com minha vida",
        "tirar a minha vida", "tirar minha vida",
        "nao quero mais viver", "nao quero viver",
        "nao aguento mais", "quero sumir", "melhor sem mim",
        "me machucar", "me cortar", "automutil", "overdose"
    )

    private val accents = Regex("\\p{Mn}+")
    private val spaces = Regex("\\s+")

    /** minúsculas, sem acento e com espaços simples: "SUICÍDIO" vira "suicidio". */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(accents, "")
            .replace(spaces, " ")
            .trim()

    fun isCrisis(text: String): Boolean {
        val n = normalize(text)
        return patterns.any { n.contains(it) }
    }
}
