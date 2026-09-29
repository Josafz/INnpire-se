import com.example.respira.util.PhoneValidation

var fails = 0
fun check(name: String, ok: Boolean) {
    println((if (ok) "OK   " else "FALHOU ") + name)
    if (!ok) fails++
}

fun main() {
    check("com DDD, 10 dígitos", PhoneValidation.isValid("1133334444"))
    check("celular com DDD, 11 dígitos", PhoneValidation.isValid("11988887777"))
    check("com máscara (11) 98888-7777", PhoneValidation.isValid("(11) 98888-7777"))
    check("com +55", PhoneValidation.isValid("+55 11 98888-7777"))
    check("vazio", !PhoneValidation.isValid(""))
    check("só 4 dígitos", !PhoneValidation.isValid("1234"))
    check("sem DDD (8 dígitos) é aceito (fixo antigo)", PhoneValidation.isValid("33334444"))
    check("14 dígitos (grande demais) rejeitado", !PhoneValidation.isValid("12345678901234"))
    check("letras sem dígito suficiente", !PhoneValidation.isValid("ligar pro joao"))
    println(if (fails == 0) "\nTODOS OS TESTES PASSARAM" else "\n$fails TESTE(S) FALHARAM")
    if (fails > 0) kotlin.system.exitProcess(1)
}
