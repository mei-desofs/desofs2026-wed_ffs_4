package com.desofs.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ForbiddenPasswordWords {

    private Set<String> words = Collections.emptySet();

    @PostConstruct
    public void load() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new ClassPathResource("security/forbidden-passwords.txt").getInputStream(),
                    java.nio.charset.StandardCharsets.UTF_8))){
            Set<String> loaded = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    loaded.add(line.toLowerCase());
                }
            }
            words = Collections.unmodifiableSet(loaded);

        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to load forbidden-passwords.txt from classpath", e);
        }
    }

    public boolean contains(String password) {
        return words.contains(password.toLowerCase());
    }
}