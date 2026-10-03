package com.devannalu.tsworkspace.common;

public class DomainProblem extends RuntimeException {
    private final int status;
    private DomainProblem(int status,String message) { super(message); this.status=status; }
    public int status() { return status; }
    public static DomainProblem missing(String message) { return new DomainProblem(404,message); }
    public static DomainProblem conflict(String message) { return new DomainProblem(409,message); }
    public static DomainProblem invalidInvite() { return new DomainProblem(400,"Convite inválido ou indisponível."); }
}
