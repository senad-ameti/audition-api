package com.audition.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.audition.model.PostComment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
class WebServiceConfigurationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void objectMapperShouldBeConfiguredCorrectly() throws JsonProcessingException {
        final PostComment comment = new PostComment(1, 2, "name", null, null);
        final String commentJson = objectMapper.writeValueAsString(comment);

        // null values not serialised
        assertThat(commentJson).doesNotContain("email", "body");

        final String json = "{\"postId\":1,\"id\":2,\"name\":\"name\", \"age\": 5}";
        final PostComment postComment = objectMapper.readValue(json, PostComment.class);
        // should not fail on unknown field age
        assertThat(postComment).isEqualTo(comment);
    }

    @Test
    void restTemplateShouldBeConfiguredCorrectly() {
        assertThat(restTemplate.getMessageConverters())
            .isNotEmpty()
            .anyMatch(converter -> converter instanceof MappingJackson2HttpMessageConverter);
    }
}
