import urllib.request, urllib.error, uuid, datetime
from inicializar import gerar_url_assinada, endpoint, bucket

chave = "validation/" + str(uuid.uuid4()) + ".txt"
conteudo = b"TS Workspace - verificacao S3 temporaria"
try:
    with urllib.request.urlopen(urllib.request.Request(gerar_url_assinada("PUT", chave), data=conteudo, method="PUT"), timeout=15) as resposta:
        assert resposta.status == 200
    with urllib.request.urlopen(urllib.request.Request(gerar_url_assinada("HEAD", chave), method="HEAD"), timeout=15) as resposta:
        assert int(resposta.headers["Content-Length"]) == len(conteudo)
    with urllib.request.urlopen(gerar_url_assinada("GET", chave), timeout=15) as resposta:
        assert resposta.read() == conteudo
    antiga = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(minutes=10)
    try:
        urllib.request.urlopen(gerar_url_assinada("GET", chave, instante=antiga), timeout=15)
        raise AssertionError("Storage aceitou URL expirada")
    except urllib.error.HTTPError as erro:
        assert erro.code == 400 and b"Date is too old" in erro.read()
    try:
        urllib.request.urlopen(endpoint + "/" + bucket + "/" + chave, timeout=15)
        raise AssertionError("Bucket permitiu leitura anonima")
    except urllib.error.HTTPError as erro:
        assert erro.code == 403
    with urllib.request.urlopen(urllib.request.Request(endpoint + "/" + bucket + "/" + chave, method="OPTIONS", headers={
        "Origin": "http://localhost:3010", "Access-Control-Request-Method": "PUT", "Access-Control-Request-Headers": "content-type"
    }), timeout=15) as resposta:
        assert resposta.headers["Access-Control-Allow-Origin"] == "http://localhost:3010"
    print("PUT/HEAD/GET assinados, conteudo, bucket privado e CORS aprovados.")
finally:
    with urllib.request.urlopen(urllib.request.Request(gerar_url_assinada("DELETE", chave), method="DELETE"), timeout=15) as resposta:
        assert resposta.status == 204
    print("DELETE e limpeza do objeto temporario aprovados.")
