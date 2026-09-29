import com.example.respira.chat.*
import com.example.respira.reminder.ReminderTime
import java.time.ZoneId
import java.time.ZonedDateTime

var fails = 0
fun check(name: String, ok: Boolean) {
    println((if (ok) "OK   " else "FALHOU ") + name)
    if (!ok) fails++
}

fun main() {
    // ---- integridade da árvore ----
    val nodes = ChatTree.allNodes()
    val ids = nodes.map { it.id }.toSet()
    check("existe nó raiz", ChatTree.ROOT in ids)
    check("existe nó de crise", ChatTree.CRISIS in ids)
    check("ids únicos", ids.size == nodes.size)
    val broken = nodes.flatMap { n -> n.options.filter { it.next !in ids }.map { "${n.id} -> ${it.next}" } }
    check("todo 'next' aponta para nó existente $broken", broken.isEmpty())
    val noOptions = nodes.filter { it.options.isEmpty() }.map { it.id }
    check("nenhum nó sem botões (beco sem saída) $noOptions", noOptions.isEmpty())
    val noText = nodes.filter { it.bot.isEmpty() }.map { it.id }
    check("todo nó tem fala do assistente $noText", noText.isEmpty())

    // alcançabilidade a partir da raiz (menos 'crise', que só entra por texto livre)
    val seen = mutableSetOf<String>()
    val stack = ArrayDeque(listOf(ChatTree.ROOT))
    while (stack.isNotEmpty()) {
        val id = stack.removeLast()
        if (seen.add(id)) ChatTree.node(id).options.forEach { stack.addLast(it.next) }
    }
    val unreachable = ids - seen - ChatTree.CRISIS
    check("todo nó é alcançável a partir do começo $unreachable", unreachable.isEmpty())

    // de qualquer nó dá para chegar ao CVV/SAMU em poucos passos (segurança)
    fun stepsToHelp(start: String): Int {
        val dist = mutableMapOf(start to 0)
        val q = ArrayDeque(listOf(start))
        while (q.isNotEmpty()) {
            val id = q.removeFirst()
            val n = ChatTree.node(id)
            if (n.options.any { it.action == ChatAction.CALL_CVV }) return dist.getValue(id)
            n.options.forEach { if (it.next !in dist) { dist[it.next] = dist.getValue(id) + 1; q.addLast(it.next) } }
        }
        return -1
    }
    val worst = ids.maxOf { stepsToHelp(it) }
    check("de qualquer nó o CVV está a no máximo 3 toques (pior caso: $worst)", worst in 0..3)
    check("node() com id inválido volta ao começo", ChatTree.node("xyz").id == ChatTree.ROOT)

    // ---- detector de crise ----
    val crise = listOf("Quero morrer", "não aguento mais", "SUICÍDIO", "vou me matar", "quero   sumir", "Penso em me machucar", "acabar com a minha vida")
    val normal = listOf("estou bem", "hoje foi um dia difícil no trabalho", "quero tomar um chá", "não consigo dormir", "estou ansioso")
    crise.forEach { check("detecta crise: \"$it\"", CrisisDetector.isCrisis(it)) }
    normal.forEach { check("não dispara em: \"$it\"", !CrisisDetector.isCrisis(it)) }
    check("normalize tira acento", CrisisDetector.normalize("  AÇÃO  Ruim ") == "acao ruim")

    // ---- horário do lembrete ----
    val z = ZoneId.of("America/Sao_Paulo")
    fun at(h: Int, m: Int, s: Int = 0) = ZonedDateTime.of(2026, 9, 28, h, m, s, 0, z)
    val hour = 3_600_000L
    check("19:00 -> 20:00 = 1h", ReminderTime.initialDelayMillis(at(19, 0), 20, 0) == hour)
    check("21:00 -> 20:00 = 23h", ReminderTime.initialDelayMillis(at(21, 0), 20, 0) == 23 * hour)
    check("20:00:00 exato -> amanhã = 24h", ReminderTime.initialDelayMillis(at(20, 0), 20, 0) == 24 * hour)
    check("19:59:30 -> 20:00 = 30s", ReminderTime.initialDelayMillis(at(19, 59, 30), 20, 0) == 30_000L)
    check("00:05 -> 08:30 = 8h25", ReminderTime.initialDelayMillis(at(0, 5), 8, 30) == 8 * hour + 25 * 60_000L)

    println(if (fails == 0) "\nTODOS OS TESTES PASSARAM" else "\n$fails TESTE(S) FALHARAM")
    if (fails > 0) kotlin.system.exitProcess(1)
}
