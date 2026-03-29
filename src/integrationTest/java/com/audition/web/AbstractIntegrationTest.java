package com.audition.web;

import com.audition.mockservers.AuditionPostMockServers;
import com.audition.utils.FileUtils;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@RequiredArgsConstructor
public class AbstractIntegrationTest {

    protected final TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void wireMockProperties(final DynamicPropertyRegistry registry) {
        AuditionPostMockServers.start();
        registry.add("audition_service_url", () -> "http://localhost:" + AuditionPostMockServers.getPort());
    }

    protected String loadResource(final String filename) throws IOException {
        return FileUtils.loadResource("response/" + filename);
    }
}
