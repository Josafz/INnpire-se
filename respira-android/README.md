# Respira v1.1 — Android nativo (Kotlin + Room)

App de apoio a momentos de ansiedade: respiração guiada (SOS), registro de humor,
dicas, **chat híbrido** e **lembrete diário**. Banco local (Room/SQLite), sem login.

## Novidades da v1.1

| Função | Como funciona | Precisa de internet? |
|---|---|---|
| Aba **Conversa** (modo guiado) | Árvore de respostas com botões, sem digitar. Pode registrar o humor no mesmo banco do Histórico | Não |
| Aba **Conversa** (modo IA, opcional) | Caixa de texto + IA via seu proxy. Só liga com internet, com aceite do usuário e com o proxy configurado | Sim |
| **Lembrete diário** (aba Dicas) | Notificação local uma vez por dia, no horário escolhido. Tocar nela abre a respiração guiada | Não |

## Estrutura

```
app/src/main/java/com/example/respira/
  MainActivity.kt          menu inferior (agora com 4 abas) e abertura pela notificação
  HomeFragment.kt          botão SOS + registro de humor
  ChatFragment.kt          NOVO  chat híbrido (guiado + IA)
  TipsFragment.kt          dicas + card do lembrete diário + CVV/SAMU
  HistoryFragment.kt       histórico e resumo de 7 dias
  SosActivity.kt           respiração guiada
  chat/
    ChatTree.kt            NOVO  árvore da conversa guiada (dados puros)
    CrisisDetector.kt      NOVO  filtro de texto de crise (nunca vai para a IA)
    AiConfig.kt            NOVO  endereço do proxy (vazio = IA desligada)
    AiClient.kt            NOVO  chamada HTTP ao proxy
  reminder/
    ReminderTime.kt        NOVO  conta do horário (pura, testada)
    ReminderScheduler.kt   NOVO  agenda com WorkManager
    ReminderWorker.kt      NOVO  roda 1x por dia
    Notifier.kt            NOVO  canal e notificação
  data/
    MoodEntry / MoodDao    humor (v1)
    ChatMessage / ChatDao  NOVO  mensagens do chat (v2)
    AppDatabase.kt         Room versão 2 com migração 1→2
  adapter/  HistoryAdapter, ChatAdapter (NOVO)
  util/     Prefs (NOVO), Dialer (NOVO)
proxy/      NOVO  servidor intermediário da IA (Flask)
tests/      NOVO  testes da lógica e do proxy
```

## Banco de dados

- `mood_entries(id, mood, timestamp)` — versão 1, igual à anterior.
- `chat_messages(id, role, text, timestamp)` — nova, versão 2.
- `AppDatabase` sobe para `version = 2` com `MIGRATION_1_2`, que só cria a tabela nova.
  **O histórico de humor já salvo não é apagado.** Não use `fallbackToDestructiveMigration`.
- O chat grava o humor na mesma tabela `mood_entries` da tela inicial.

## Configurar a IA online (opcional)

O app funciona sem isso; a chave "IA online" só aparece se `AiConfig.PROXY_URL` estiver preenchida.

1. `cd proxy && pip install -r requirements.txt`
2. Defina a chave: `export ANTHROPIC_API_KEY="..."` (PowerShell: `$env:ANTHROPIC_API_KEY="..."`)
3. `flask --app server run --host 0.0.0.0 --port 8000`
4. Em `chat/AiConfig.kt`, coloque `PROXY_URL = "http://10.0.2.2:8000"` (emulador → PC).
5. Celular físico: HTTP puro só é aceito para `10.0.2.2` e `localhost` (ver
   `res/xml/network_security_config.xml`). Para uso real, publique o proxy com **HTTPS**.

Por que um proxy: a chave da API não pode ficar dentro do APK, qualquer um consegue ler.
Antes de publicar de verdade, o proxy precisa de limite de uso por usuário e autenticação.

## Segurança e privacidade (leia antes de usar com pessoas reais)

- Texto livre com sinais de crise **não** é enviado à IA: o app responde com CVV 188 / SAMU 192.
  O detector é uma busca por trechos: pode deixar passar frases que não previmos e pode
  disparar por engano. Ele é uma rede de segurança, não um classificador.
- Com a IA ligada, as últimas mensagens da conversa saem do aparelho. Dados de saúde são
  dados pessoais sensíveis pela LGPD: confirme com o professor/responsável o que é exigido.
- O app não substitui acompanhamento profissional (aviso na tela de Dicas e no aceite da IA).

## Lembrete diário: limites

- Usa WorkManager: funciona offline e sobrevive a reiniciar o celular, mas **não é relógio exato**;
  o Android pode adiantar ou atrasar alguns minutos para poupar bateria.
- Android 13+ pede permissão de notificações; sem ela o lembrete não liga.

## Testes

- Lógica pura (árvore do chat, detector de crise, horário do lembrete), 27 verificações:
  `kotlinc tests/logica/Main.kt app/src/main/java/com/example/respira/chat/ChatTree.kt app/src/main/java/com/example/respira/chat/CrisisDetector.kt app/src/main/java/com/example/respira/reminder/ReminderTime.kt -include-runtime -d t.jar && java -jar t.jar`
- Proxy (com a API simulada): `pip install flask requests && python tests/test_proxy.py`
- Próximo passo natural: converter estes testes para JUnit em `app/src/test/` e rodar com `./gradlew test`.

## Rodar o app

Abra a pasta no Android Studio, aguarde o Gradle sincronizar e clique em Run (▶).
A primeira compilação baixa as dependências (inclui `androidx.work`).
Sem o Android Studio: JDK 17 + Android SDK, `gradle wrapper`, `./gradlew assembleDebug`.

## Som da tela SOS (opcional)

Coloque `calm_loop.mp3` em `app/src/main/res/raw/`. Sem o arquivo, o app funciona, só sem som.

## v1.2 — Contatos de confiança

3 contatos (nome + telefone) cadastrados na aba Dicas. Cada um tem dois botões:

- **Ligar** — abre o discador com o número já preenchido (`ACTION_DIAL`, sem permissão).
- **SMS** — abre o app de mensagens com o texto "Oi, pode me ligar? Preciso conversar." já
  preenchido; o usuário revê e confirma antes de enviar (`ACTION_SENDTO`, sem permissão).

Nenhum dos dois liga ou envia sozinho — é sempre o usuário quem confirma no app padrão do
celular. Dá para trocar por envio silencioso com `SmsManager.sendTextMessage()`, mas isso pede
a permissão perigosa `SEND_SMS` e manda a mensagem sem o usuário ver antes; mantive o padrão
mais seguro, igual ao `Dialer` que já existia.

Novos arquivos: `data/EmergencyContact.kt`, `data/EmergencyContactDao.kt`,
`EmergencyContactsActivity.kt` (tela de cadastro), `util/Sms.kt`, `util/PhoneValidation.kt`
(testado em `tests/logica/PhoneValidationTest.kt`). Banco sobe para `version = 3`
(`MIGRATION_2_3`, tabela `emergency_contacts`), testada com SQLite real do mesmo jeito que a
migração 1→2 (histórico de humor e conversa preservados).
