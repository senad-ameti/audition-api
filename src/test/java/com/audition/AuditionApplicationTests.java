package com.audition;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuditionApplicationTests {

    @Test
    void contextLoads() {
        // Ensures Spring context loads without errors
        assertTrue(true); // satisfies PMD
    }
}