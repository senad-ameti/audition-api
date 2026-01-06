package com.audition;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class AuditionApplicationTests {

    // implement unit test. Note that an applicant should create additional unit tests as required.

    @Test
    void contextLoads() {
        ApplicationContext ctx = SpringApplication.run(AuditionApplication.class, new String[]{});
        assertNotNull(ctx);
    }
}
