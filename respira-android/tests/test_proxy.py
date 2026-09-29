import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "proxy"))
from unittest import mock
import server

c = server.app.test_client()
fails = 0
def check(name, ok):
    global fails
    print(("OK    " if ok else "FALHOU ") + name)
    fails += 0 if ok else 1

class FakeResp:
    def __init__(self, status=200, body=None): self.status_code = status; self._b = body
    def json(self): return self._b

# sem chave
os.environ.pop("ANTHROPIC_API_KEY", None)
r = c.post("/chat", json={"messages": [{"role": "user", "content": "oi"}]})
check("sem chave configurada -> 500", r.status_code == 500)

os.environ["ANTHROPIC_API_KEY"] = "chave-de-teste"

# validações
check("corpo vazio -> 400", c.post("/chat", json={}).status_code == 400)
check("messages não é lista -> 400", c.post("/chat", json={"messages": "oi"}).status_code == 400)
check("papel inválido -> 400", c.post("/chat", json={"messages": [{"role": "system", "content": "x"}]}).status_code == 400)
check("conteúdo vazio -> 400", c.post("/chat", json={"messages": [{"role": "user", "content": "  "}]}).status_code == 400)
check("só mensagens do assistente -> 400", c.post("/chat", json={"messages": [{"role": "assistant", "content": "oi"}]}).status_code == 400)
check("GET /health", c.get("/health").get_json() == {"ok": True})

# normalização
m = server.clean_messages([
    {"role": "assistant", "content": "Oi! Como você está?"},   # começa pelo assistente: descartada
    {"role": "user", "content": "Estou ansioso"},
    {"role": "user", "content": "muito"},                      # user seguido de user: junta
    {"role": "assistant", "content": "Sinto muito."},
    {"role": "user", "content": "x" * 5000},                   # limite de tamanho
])
check("descarta assistente inicial", m[0]["role"] == "user")
check("junta papéis repetidos", m[0]["content"] == "Estou ansioso\nmuito")
check("alterna user/assistant/user", [x["role"] for x in m] == ["user", "assistant", "user"])
check("corta mensagem gigante em 1000", len(m[2]["content"]) == server.MAX_CHARS)
many = [{"role": "user" if i % 2 == 0 else "assistant", "content": f"m{i}"} for i in range(40)]
check("mantém no máximo 12 mensagens", len(server.clean_messages(many)) <= server.MAX_MESSAGES)

# chamada à IA (simulada): confere o que o servidor ENVIA e o que devolve
with mock.patch("server.requests.post") as post:
    post.return_value = FakeResp(200, {"content": [{"type": "text", "text": "Respire devagar. Estou aqui."}]})
    r = c.post("/chat", json={"messages": [{"role": "user", "content": "estou ansioso"}]})
    check("resposta 200 com reply", r.status_code == 200 and r.get_json()["reply"] == "Respire devagar. Estou aqui.")
    kw = post.call_args.kwargs
    check("envia x-api-key", kw["headers"]["x-api-key"] == "chave-de-teste")
    check("envia anthropic-version", kw["headers"]["anthropic-version"] == "2023-06-01")
    check("envia modelo, max_tokens e system", kw["json"]["model"] == server.MODEL and kw["json"]["max_tokens"] == 400 and "CVV" in kw["json"]["system"])
    check("envia as mensagens limpas", kw["json"]["messages"] == [{"role": "user", "content": "estou ansioso"}])
    check("chave NÃO vai no corpo nem volta ao app", "chave-de-teste" not in str(kw["json"]) and "chave-de-teste" not in r.get_data(as_text=True))

    post.return_value = FakeResp(200, {"content": []})
    check("resposta vazia da IA -> texto padrão", c.post("/chat", json={"messages": [{"role": "user", "content": "oi"}]}).get_json()["reply"] == server.FALLBACK_REPLY)

    post.return_value = FakeResp(529, {})
    check("erro da IA -> 502", c.post("/chat", json={"messages": [{"role": "user", "content": "oi"}]}).status_code == 502)

    import requests
    post.side_effect = requests.ConnectionError()
    check("sem rede até a IA -> 502", c.post("/chat", json={"messages": [{"role": "user", "content": "oi"}]}).status_code == 502)

print("\nTODOS PASSARAM" if fails == 0 else f"\n{fails} FALHARAM")
sys.exit(1 if fails else 0)
