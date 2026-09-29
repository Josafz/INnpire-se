"""
Proxy do chat com IA do Respira.

Por que existe: a chave da API NÃO pode ficar dentro do app (qualquer pessoa consegue abrir
um APK e ler o que está nele). O app fala só com este servidor; este servidor, que guarda a
chave numa variável de ambiente, fala com a IA.

Rodar (no PC):
    pip install -r requirements.txt
    export ANTHROPIC_API_KEY="sua-chave"        # Windows PowerShell: $env:ANTHROPIC_API_KEY="sua-chave"
    flask --app server run --host 0.0.0.0 --port 8000

Contrato com o app:
    POST /chat   {"messages": [{"role": "user"|"assistant", "content": "..."}]}
    resposta     {"reply": "..."}
"""
import os

import requests
from flask import Flask, jsonify, request

API_URL = "https://api.anthropic.com/v1/messages"
API_VERSION = "2023-06-01"
MODEL = os.environ.get("AI_MODEL", "claude-haiku-4-5-20251001")

MAX_MESSAGES = 12   # só o contexto recente é usado
MAX_CHARS = 1000    # limite por mensagem (protege custo e abuso)
FALLBACK_REPLY = "Estou aqui com você. Quer tentar respirar comigo por um minuto?"

SYSTEM_PROMPT = (
    "Você é o assistente de acolhimento do app Respira, voltado a pessoas com ansiedade, "
    "principalmente com 50 anos ou mais. Responda sempre em português do Brasil, com frases "
    "curtas, tom calmo e simples, em no máximo 4 frases. Ofereça apoio emocional geral e técnicas "
    "simples (respiração, atenção ao presente, pausas). Não faça diagnóstico, não indique "
    "medicamentos e não substitua um profissional de saúde. Se a pessoa falar em se machucar, "
    "morrer ou estiver em crise, acolha e oriente a ligar para o CVV (188, 24 horas, gratuito) "
    "ou para o SAMU (192) em emergência. Não peça dados pessoais."
)

app = Flask(__name__)


def clean_messages(raw):
    """Valida e arruma o histórico para o formato que a API exige. Devolve None se for inválido."""
    if not isinstance(raw, list):
        return None
    msgs = []
    for m in raw[-MAX_MESSAGES:]:
        if not isinstance(m, dict):
            return None
        role, content = m.get("role"), m.get("content")
        if role not in ("user", "assistant") or not isinstance(content, str) or not content.strip():
            return None
        content = content.strip()[:MAX_CHARS]
        if msgs and msgs[-1]["role"] == role:          # papéis iguais seguidos: junta numa só
            msgs[-1]["content"] += "\n" + content
        else:
            msgs.append({"role": role, "content": content})
    while msgs and msgs[0]["role"] != "user":           # a conversa precisa começar pelo usuário
        msgs.pop(0)
    return msgs or None


@app.get("/health")
def health():
    return jsonify(ok=True)


@app.post("/chat")
def chat():
    api_key = os.environ.get("ANTHROPIC_API_KEY", "")
    if not api_key:
        return jsonify(error="servidor sem chave configurada"), 500

    data = request.get_json(silent=True) or {}
    msgs = clean_messages(data.get("messages"))
    if msgs is None:
        return jsonify(error="mensagens inválidas"), 400

    try:
        resp = requests.post(
            API_URL,
            headers={
                "x-api-key": api_key,
                "anthropic-version": API_VERSION,
                "content-type": "application/json",
            },
            json={"model": MODEL, "max_tokens": 400, "system": SYSTEM_PROMPT, "messages": msgs},
            timeout=30,
        )
    except requests.RequestException:
        return jsonify(error="falha ao falar com a IA"), 502

    if resp.status_code != 200:
        return jsonify(error="a IA recusou o pedido"), 502

    blocks = resp.json().get("content", [])
    text = "".join(b.get("text", "") for b in blocks if b.get("type") == "text").strip()
    return jsonify(reply=text or FALLBACK_REPLY)
