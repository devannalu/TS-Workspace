package com.devannalu.tsworkspace.common;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(DomainProblem.class)
    ProblemDetail domainProblem(DomainProblem error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(error.status()), error.getMessage());
    }
    @ExceptionHandler(com.devannalu.tsworkspace.teams.TeamProblem.class)
    ProblemDetail teamProblem(com.devannalu.tsworkspace.teams.TeamProblem error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(error.status()), error.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ProblemDetail conflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflito com os dados existentes.");
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ProblemDetail malformedRequest() { return invalidRequest(); }
    @ExceptionHandler(DataAccessException.class)
    ProblemDetail databaseUnavailable() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Serviço temporariamente indisponível.");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail invalidCredentials() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidRequest() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos.");
    }
}
