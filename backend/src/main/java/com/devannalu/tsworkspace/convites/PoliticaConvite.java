package com.devannalu.tsworkspace.convites;

import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.HexFormat;

public final class PoliticaConvite {
    public static final int PRAZO_PADRAO_CONVITE_DIAS=7;
    private static final SecureRandom GERADOR_ALEATORIO=new SecureRandom();
    private PoliticaConvite() { }
    public enum SituacaoConvite { PENDING, USED, CANCELLED, EXPIRED }
    public static String gerarToken() { byte[] bytes=new byte[32];GERADOR_ALEATORIO.nextBytes(bytes);return HexFormat.of().formatHex(bytes); }
    public static String calcularHashToken(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("Algoritmo indisponível.");}
    }
    public static boolean possuiFormatoValido(String token) { return token!=null&&token.matches("[a-fA-F0-9]{64}"); }
    public static SituacaoConvite calcularSituacao(Instant used,Instant cancelled,Instant expires,Instant now) {
        if(used!=null)return SituacaoConvite.USED;
        if(cancelled!=null)return SituacaoConvite.CANCELLED;
        return expires.isAfter(now)?SituacaoConvite.PENDING:SituacaoConvite.EXPIRED;
    }
    public static void exigirConvitePendente(Instant used,Instant cancelled,Instant expires,Instant now) {
        if(calcularSituacao(used,cancelled,expires,now)!=SituacaoConvite.PENDING)throw ProblemaDominio.conviteIndisponivel();
    }
    public static void validarAceiteConvite(String name,String password,String confirmation) {
        if(name==null||name.trim().length()<2||name.trim().length()>100||password==null||password.length()<12||password.length()>128
            ||password.getBytes(StandardCharsets.UTF_8).length>72||!password.equals(confirmation))throw new IllegalArgumentException();
    }
}
