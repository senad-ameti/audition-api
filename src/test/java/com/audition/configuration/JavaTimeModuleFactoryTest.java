package com.audition.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class JavaTimeModuleFactoryTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(JavaTimeModuleFactory.create());

    @Test
    void create() throws JsonProcessingException {
        assertInstanceOf(JavaTimeModule.class, JavaTimeModuleFactory.create());
        final String json = mapper.writeValueAsString(LocalDate.of(2024, 6, 15));
        assertEquals("\"2024-06-15\"", json);

        assertEquals("\"2024-06-15\"", mapper.writeValueAsString(LocalDate.of(2024, 6, 15)));
        assertEquals(LocalDate.of(2024, 6, 15), mapper.readValue("\"2024-06-15\"", LocalDate.class));
        assertEquals("\"2024-06-15T10:30:00\"",
            mapper.writeValueAsString(LocalDateTime.of(2024, 6, 15, 10, 30, 0)));
        assertEquals(LocalDateTime.of(2024, 6, 15, 10, 30, 0),
            mapper.readValue("\"2024-06-15T10:30:00\"", LocalDateTime.class));

    }
}
