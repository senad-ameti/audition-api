package com.audition.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

class SystemExceptionTest {

    @Test
    void shouldCreateCorrectException() {
        assertThat(new SystemException("detail", 400, new IllegalArgumentException("invalid input"))).satisfies(
            systemException -> {
                assertThat(systemException.getDetail()).isEqualTo("detail");
                assertThat(systemException.getTitle()).isEqualTo("API Error Occurred");
                assertThat(systemException.getStatusCode()).isEqualTo(400);
            }
        );

        assertThat(new SystemException("detail", "title", 500,
            new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))).satisfies(
            systemException -> {
                assertThat(systemException.getDetail()).isEqualTo("detail");
                assertThat(systemException.getTitle()).isEqualTo("title");
                assertThat(systemException.getStatusCode()).isEqualTo(500);
            }
        );
    }

}
