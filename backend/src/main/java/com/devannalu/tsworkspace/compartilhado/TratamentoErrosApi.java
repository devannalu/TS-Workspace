package com.devannalu.tsworkspace.compartilhado;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratamentoErrosApi {
    @ExceptionHandler(ProblemaDominio.class)
    ProblemDetail problemaDominio(ProblemaDominio erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(erro.status()), erro.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ProblemDetail conflitoPersistencia() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflito com os dados existentes.");
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ProblemDetail requisicaoMalformada() { return requisicaoInvalida(); }
    @ExceptionHandler(DataAccessException.class)
    ProblemDetail bancoIndisponivel() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Serviço temporariamente indisponível.");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail credenciaisInvalidas() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail requisicaoInvalida() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail falhaValidacao() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos.");
    }
}
