package com.audition.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class FileUtils {

    private FileUtils() {}

    /**
     * Loads a resource file from the classpath.
     *
     * @param path the resource path relative to the classpath
     * @return the content of the resource file as a String
     * @throws IOException if the resource is not found or cannot be read
     */
    public static String loadResource(final String path) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
