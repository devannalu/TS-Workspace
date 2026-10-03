package com.devannalu.tsworkspace.compartilhado;

public class ProblemaDominio extends RuntimeException {
    private final int status;
    private ProblemaDominio(int status,String mensagem) { super(mensagem); this.status=status; }
    public int status() { return status; }
    public static ProblemaDominio naoEncontrado(String mensagem) { return new ProblemaDominio(404,mensagem); }
    public static ProblemaDominio conflito(String mensagem) { return new ProblemaDominio(409,mensagem); }
    public static ProblemaDominio conviteIndisponivel() { return new ProblemaDominio(400,"Convite inválido ou indisponível."); }
}
