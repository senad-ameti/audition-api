package com.audition.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class JavaTimeModuleFactoryTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(JavaTimeModuleFactory.create());

    @Test
    void localDateSerializesAsIsoString() throws JsonProcessingException {
        final String json = mapper.writeValueAsString(LocalDate.of(2024, 6, 15));
        assertThat(json).isEqualTo("\"2024-06-15\"");
    }

    @Test
    void localDateDeserializesFromIsoString() throws JsonProcessingException {
        final LocalDate date = mapper.readValue("\"2024-06-15\"", LocalDate.class);
        assertThat(date).isEqualTo(LocalDate.of(2024, 6, 15));
    }

    @Test
    void localDateTimeSerializesAsIsoString() throws JsonProcessingException {
        final String json = mapper.writeValueAsString(LocalDateTime.of(2024, 6, 15, 10, 30, 0));
        assertThat(json).isEqualTo("\"2024-06-15T10:30:00\"");
    }

    @Test
    void localDateTimeDeserializesFromIsoString() throws JsonProcessingException {
        final LocalDateTime dt = mapper.readValue("\"2024-06-15T10:30:00\"", LocalDateTime.class);
        assertThat(dt).isEqualTo(LocalDateTime.of(2024, 6, 15, 10, 30, 0));
    }

    @Test
    void createReturnsJavaTimeModule() {
        assertThat(JavaTimeModuleFactory.create()).isInstanceOf(JavaTimeModule.class);
    }
}
