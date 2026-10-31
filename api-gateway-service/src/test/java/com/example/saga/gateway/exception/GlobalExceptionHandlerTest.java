package com.example.saga.gateway.exception;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleReactiveValidationExceptions_ReturnsBadRequestWithFieldErrors() {
        WebExchangeBindException ex = mock(WebExchangeBindException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "field", "must not be blank")));

        var response = handler.handleReactiveValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("field", "must not be blank");
    }

    @Test
    void handleDependencyFailure_ReturnsServiceUnavailable() {
        var response = handler.handleDependencyFailure(new DependencyUnavailableException("downstream", new RuntimeException("cause")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().error()).isEqualTo("DEPENDENCY_FAILURE");
    }

    @Test
    void handleBadRequest_ReturnsBadRequest() {
        var response = handler.handleBadRequest(new IllegalArgumentException("invalid input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("invalid input");
    }

    @Test
    void handleGenericException_ReturnsInternalServerError() {
        var response = handler.handleGenericException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
