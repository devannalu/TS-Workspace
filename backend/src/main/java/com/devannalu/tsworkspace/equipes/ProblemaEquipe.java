package com.devannalu.tsworkspace.equipes;

public class ProblemaEquipe extends RuntimeException {
    private final int status;
    private ProblemaEquipe(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
    public static ProblemaEquipe naoEncontrada(String message) { return new ProblemaEquipe(404, message); }
    public static ProblemaEquipe conflito(String message) { return new ProblemaEquipe(409, message); }
}
