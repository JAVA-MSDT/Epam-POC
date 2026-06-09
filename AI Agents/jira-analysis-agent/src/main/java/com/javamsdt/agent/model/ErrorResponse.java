package com.javamsdt.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ErrorResponse(
        @JsonProperty("status") int status,
        @JsonProperty("error") String error,
        @JsonProperty("message") String message,
        @JsonProperty("timestamp") LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}
