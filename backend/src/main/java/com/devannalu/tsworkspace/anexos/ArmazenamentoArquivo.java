package com.devannalu.tsworkspace.anexos;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;

@Component
public class ArmazenamentoArquivo implements AutoCloseable {
    public static final long LIMITE=10*1024*1024;
    private final S3Client cliente;
    private final S3Presigner assinador;
    private final String bucket;
    private final Duration validade;
    public ArmazenamentoArquivo(@Value("${STORAGE_ENDPOINT:http://localhost:3900}") String endpoint,
        @Value("${STORAGE_REGION:garage}") String regiao,@Value("${STORAGE_BUCKET:ts-workspace-dev}") String bucket,
        @Value("${STORAGE_ACCESS_KEY:nao-configurada}") String chave,@Value("${STORAGE_SECRET_KEY:nao-configurada}") String segredo,
        @Value("${STORAGE_URL_SECONDS:300}") long segundos) {
        if(segundos<1||segundos>300)throw new IllegalArgumentException("Validade de storage deve estar entre 1 e 300 segundos.");
        this.bucket=bucket;validade=Duration.ofSeconds(segundos);
        var credenciais=StaticCredentialsProvider.create(AwsBasicCredentials.create(chave,segredo));
        var config=S3Configuration.builder().pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build();
        cliente=S3Client.builder().requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED).endpointOverride(URI.create(endpoint)).region(Region.of(regiao))
            .credentialsProvider(credenciais).serviceConfiguration(config).httpClientBuilder(UrlConnectionHttpClient.builder())
            .overrideConfiguration(c->c.apiCallTimeout(Duration.ofSeconds(30)).apiCallAttemptTimeout(Duration.ofSeconds(15)))
            .build();
        assinador=S3Presigner.builder().endpointOverride(URI.create(endpoint)).region(Region.of(regiao))
            .credentialsProvider(credenciais).serviceConfiguration(config).build();
    }
    public Duration validade() { return validade; }
    public record Upload(String url,Map<String,String> headers) { }
    public Upload criarUpload(Anexo anexo) {
        var pedido=PutObjectRequest.builder().bucket(bucket).key(temporaria(anexo)).contentType(anexo.tipoMime())
            .contentLength(anexo.tamanhoBytes()).build();
        Duration restante=Duration.between(java.time.Instant.now(),anexo.uploadExpiraEm());
        if(restante.getSeconds()<1)throw invalido("A autorização de upload expirou.");
        var assinado=assinador.presignPutObject(PutObjectPresignRequest.builder().signatureDuration(restante.compareTo(validade)<0?restante:validade).putObjectRequest(pedido).build());
        return new Upload(assinado.url().toString(),Map.of("Content-Type",anexo.tipoMime()));
    }
    private String temporaria(Anexo a) { return "pending/"+a.chaveObjeto(); }
    // A chave definitiva nunca recebe uma URL de escrita: evita substituição após confirmar.
    public void confirmarUpload(Anexo a) {
        try {
            var cabecalho=cliente.headObject(HeadObjectRequest.builder().bucket(bucket).key(temporaria(a)).build());
            if(cabecalho.contentLength()!=a.tamanhoBytes()||cabecalho.contentLength()>LIMITE)
                throw invalido("O tamanho do arquivo não corresponde ao upload autorizado.");
            byte[] bytes;
            try(var entrada=cliente.getObject(GetObjectRequest.builder().bucket(bucket).key(temporaria(a)).build())) {
                bytes=entrada.readNBytes((int)LIMITE+1);
            }
            if(bytes.length!=a.tamanhoBytes()||!conteudoValido(bytes,a.tipoMime()))
                throw invalido("O conteúdo do arquivo não corresponde ao tipo permitido.");
            cliente.putObject(PutObjectRequest.builder().bucket(bucket).key(a.chaveObjeto()).contentType(a.tipoMime()).build(),RequestBody.fromBytes(bytes));
        } catch(S3Exception e) {
            if(e.statusCode()==404)throw invalido("O upload ainda não foi encontrado. Tente novamente.");
            registrarFalha(e);throw indisponivel();
        }
          catch(ProblemaDominio e) { throw e; }
          catch(Exception e) { registrarFalha(e); throw indisponivel(); }
    }
    public String gerarDownload(Anexo a) {
        try {
            cliente.headObject(HeadObjectRequest.builder().bucket(bucket).key(a.chaveObjeto()).build());
            String nome=java.net.URLEncoder.encode(a.nomeOriginal(),java.nio.charset.StandardCharsets.UTF_8).replace("+","%20");
            var pedido=GetObjectRequest.builder().bucket(bucket).key(a.chaveObjeto()).responseContentType("application/octet-stream")
                .responseContentDisposition("attachment; filename*=UTF-8''"+nome).build();
            return assinador.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(validade).getObjectRequest(pedido).build()).url().toString();
        } catch(S3Exception e) {
            if(e.statusCode()==404)throw ProblemaDominio.naoEncontrado("Arquivo não encontrado.");
            registrarFalha(e);throw indisponivel();
        }
          catch(ProblemaDominio e) { throw e; }
          catch(Exception e) { registrarFalha(e); throw indisponivel(); }
    }
    public void removerObjeto(Anexo a) {
        try {
            cliente.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(a.chaveObjeto()).build());
            cliente.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(temporaria(a)).build());
        } catch(Exception e) { registrarFalha(e); throw indisponivel(); }
    }
    static boolean conteudoValido(byte[] bytes,String mime) {
        if(bytes.length==0)return false;
        return switch(mime) {
            case "application/pdf" -> inicia(bytes,new byte[]{37,80,68,70,45});
            case "image/png" -> inicia(bytes,new byte[]{(byte)137,80,78,71,13,10,26,10});
            case "image/jpeg" -> inicia(bytes,new byte[]{(byte)255,(byte)216,(byte)255});
            case "image/webp" -> bytes.length>12&&inicia(bytes,new byte[]{82,73,70,70})&&new String(bytes,8,4,StandardCharsets.US_ASCII).equals("WEBP");
            case "text/plain","text/csv" -> textoValido(bytes);
            default -> false;
        };
    }
    private static boolean inicia(byte[] b,byte[] assinatura) {
        if(b.length<assinatura.length)return false;
        for(int i=0;i<assinatura.length;i++)if(b[i]!=assinatura[i])return false;
        return true;
    }
    private static boolean textoValido(byte[] b) {
        try {
            String texto=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b)).toString();
            if(texto.codePoints().anyMatch(c->c<32&&c!=9&&c!=10&&c!=13))return false;
            String inicio=texto.stripLeading().toLowerCase(Locale.ROOT);
            return !inicio.startsWith("#!")&&!inicio.startsWith("mz")&&!inicio.contains("<script")
                &&!inicio.contains("<html")&&!inicio.contains("<svg")&&!inicio.contains("<?php");
        } catch(CharacterCodingException e) { return false; }
    }
    private void registrarFalha(Exception e) {
        String codigo=e instanceof S3Exception s && s.awsErrorDetails()!=null?s.awsErrorDetails().errorCode():e.getClass().getSimpleName();
        org.slf4j.LoggerFactory.getLogger(ArmazenamentoArquivo.class).warn("Falha no armazenamento: {}",codigo);
    }
    private ProblemaDominio invalido(String mensagem) { return ProblemaDominio.conflito(mensagem); }
    private ProblemaDominio indisponivel() { return ProblemaDominio.indisponivel("Armazenamento indisponível. Tente novamente."); }
    @Override public void close() { cliente.close(); assinador.close(); }
}
