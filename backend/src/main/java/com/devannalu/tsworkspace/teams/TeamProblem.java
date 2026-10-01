package com.devannalu.tsworkspace.teams;

public class TeamProblem extends RuntimeException {
    private final int status;
    private TeamProblem(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
    public static TeamProblem missing(String message) { return new TeamProblem(404, message); }
    public static TeamProblem conflict(String message) { return new TeamProblem(409, message); }
}
