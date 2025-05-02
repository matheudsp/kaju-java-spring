package com.valedosol.kaju.feature.whatsapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class EvolutionApiException extends RuntimeException {
    public EvolutionApiException(String message) {
        super(message);
    }
    
    public EvolutionApiException(String message, Throwable cause) {
        super(message, cause);
    }
}