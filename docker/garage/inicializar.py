import hashlib,hmac,datetime,urllib.parse,urllib.request
from pathlib import Path
import subprocess,time
raiz=Path(__file__).resolve().parents[2]
import os
os.chdir(raiz)
configuracao={}
for linha in Path('.env').read_text(encoding='utf-8').splitlines():
 if '=' in linha and not linha.lstrip().startswith('#'):
  nome_variavel,valor=linha.split('=',1);configuracao[nome_variavel.strip()]=valor.strip().strip(chr(34)).strip(chr(39))
endpoint=configuracao['STORAGE_ENDPOINT']; bucket=configuracao['STORAGE_BUCKET'];region=configuracao['STORAGE_REGION']
if urllib.parse.urlparse(endpoint).hostname not in ("localhost", "127.0.0.1"):
 raise RuntimeError("Inicialização do Garage aceita somente endpoint local de desenvolvimento.")

subprocess.run(['docker','compose','up','-d','garage'],check=True)
for tentativa in range(40):
 resultado=subprocess.run(['docker','inspect','ts-workspace-garage','--format','{{.State.Health.Status}}'],capture_output=True,text=True,check=True)
 if resultado.stdout.strip()=='healthy':break
 time.sleep(1)
else:raise RuntimeError('Garage não ficou saudável.')

def gerar_url_assinada(method,key='',extra=None,instante=None):
 now=instante or datetime.datetime.now(datetime.timezone.utc);date=now.strftime('%Y%m%d');stamp=now.strftime('%Y%m%dT%H%M%SZ')
 scope=f'{date}/{region}/s3/aws4_request';host=urllib.parse.urlparse(endpoint).netloc
 path='/'+bucket+('/'+urllib.parse.quote(key,safe='/') if key else '')
 q={'X-Amz-Algorithm':'AWS4-HMAC-SHA256','X-Amz-Credential':configuracao['STORAGE_ACCESS_KEY']+'/'+scope,'X-Amz-Date':stamp,'X-Amz-Expires':'300','X-Amz-SignedHeaders':'host'}
 if extra:q.update(extra)
 query=urllib.parse.urlencode(sorted(q.items()),quote_via=urllib.parse.quote)
 canonical=method+'\n'+path+'\n'+query+'\nhost:'+host+'\n\nhost\nUNSIGNED-PAYLOAD'
 tosign='AWS4-HMAC-SHA256\n'+stamp+'\n'+scope+'\n'+hashlib.sha256(canonical.encode()).hexdigest()
 signing=('AWS4'+configuracao['STORAGE_SECRET_KEY']).encode()
 for part in [date,region,'s3','aws4_request']:signing=hmac.new(signing,part.encode(),hashlib.sha256).digest()
 signature=hmac.new(signing,tosign.encode(),hashlib.sha256).hexdigest()
 return endpoint+path+'?'+query+'&X-Amz-Signature='+signature
cors=b'<CORSConfiguration xmlns="http://s3.amazonaws.com/doc/2006-03-01/"><CORSRule><AllowedOrigin>http://localhost:3010</AllowedOrigin><AllowedMethod>PUT</AllowedMethod><AllowedMethod>GET</AllowedMethod><AllowedMethod>HEAD</AllowedMethod><AllowedHeader>content-type</AllowedHeader><ExposeHeader>ETag</ExposeHeader><MaxAgeSeconds>300</MaxAgeSeconds></CORSRule></CORSConfiguration>'
with urllib.request.urlopen(urllib.request.Request(gerar_url_assinada('PUT',extra={'cors':''}),data=cors,method='PUT'),timeout=15) as r:assert r.status==200
print('Garage saudável; bucket privado configuracao CORS local configurados.')
