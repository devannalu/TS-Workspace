package com.devannalu.tsworkspace.compartilhado;

import java.time.*;

public final class HorarioEncontro {
    private HorarioEncontro() { }
    public static Instant resolver(LocalDateTime data,String zona) {
        if(data==null||zona==null)throw new IllegalArgumentException();
        final ZoneId fuso;try {fuso=ZoneId.of(zona);}catch(DateTimeException e){throw new IllegalArgumentException();}
        var offsets=fuso.getRules().getValidOffsets(data);
        // Não escolher silenciosamente um horário inexistente ou duplicado na transição de verão.
        if(offsets.size()!=1)throw ProblemaDominio.requisicaoInvalida("Horário inexistente ou ambíguo neste fuso. Escolha outro horário.");
        return data.toInstant(offsets.get(0));
    }
}
