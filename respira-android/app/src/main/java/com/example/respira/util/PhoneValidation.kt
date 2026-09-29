package com.example.respira.util

/**
 * Validação simples do telefone dos contatos de confiança: pura (sem Android),
 * então dá para testar isolada, fora do aparelho.
 */
object PhoneValidation {
    /** Aceita como a pessoa digitou (com ou sem parênteses/traço); só confere a quantidade de dígitos. */
    fun isValid(phone: String): Boolean {
        val digits = phone.count { it.isDigit() }
        return digits in 8..13   // fixo com DDD (10) até celular com +55 e DDD (13)
    }
}
