package com.devannalu.tsworkspace.invites;

import com.devannalu.tsworkspace.common.DomainProblem;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.HexFormat;

public final class InvitePolicy {
    public static final int TTL_DAYS=7;
    private static final SecureRandom RANDOM=new SecureRandom();
    private InvitePolicy() { }
    public enum Status { PENDING, USED, CANCELLED, EXPIRED }
    public static String token() { byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return HexFormat.of().formatHex(bytes); }
    public static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("Algoritmo indisponível.");}
    }
    public static boolean validFormat(String token) { return token!=null&&token.matches("[a-fA-F0-9]{64}"); }
    public static Status status(Instant used,Instant cancelled,Instant expires,Instant now) {
        if(used!=null)return Status.USED;
        if(cancelled!=null)return Status.CANCELLED;
        return expires.isAfter(now)?Status.PENDING:Status.EXPIRED;
    }
    public static void pending(Instant used,Instant cancelled,Instant expires,Instant now) {
        if(status(used,cancelled,expires,now)!=Status.PENDING)throw DomainProblem.invalidInvite();
    }
    public static void acceptance(String name,String password,String confirmation) {
        if(name==null||name.trim().length()<2||name.trim().length()>100||password==null||password.length()<12||password.length()>128
            ||password.getBytes(StandardCharsets.UTF_8).length>72||!password.equals(confirmation))throw new IllegalArgumentException();
    }
}
