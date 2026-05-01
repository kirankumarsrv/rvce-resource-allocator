package com.rvce.scas.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Test configuration that provides a mocked RedisTemplate for integration tests.
 * Uses an in-memory HashMap to simulate Redis behavior for testing purposes.
 */
@TestConfiguration
@Profile("test")
public class EmbeddedRedisConfig {

    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = mock(RedisTemplate.class);
        Map<String, String> inMemoryStore = new HashMap<>();

        // Mock opsForValue() to return a working ValueOperations
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            inMemoryStore.put(key, value);
            return null;
        }).when(valueOps).set(anyString(), anyString());

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return inMemoryStore.get(key);
        }).when(valueOps).get(anyString());

        // Mock keys() to return all keys
        doAnswer(invocation -> {
            String pattern = invocation.getArgument(0);
            if ("*".equals(pattern)) {
                return inMemoryStore.keySet();
            }
            return Set.of();
        }).when(template).keys(anyString());

        // Mock delete()
        doAnswer(invocation -> {
            Object keysArg = invocation.getArgument(0);
            if (keysArg instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> keys = (Set<String>) keysArg;
                long deletedCount = keys.stream().filter(inMemoryStore::containsKey).count();
                keys.forEach(inMemoryStore::remove);
                return deletedCount;
            } else if (keysArg instanceof String) {
                String key = (String) keysArg;
                boolean existed = inMemoryStore.containsKey(key);
                inMemoryStore.remove(key);
                return existed ? 1L : 0L;
            }
            return 0L;
        }).when(template).delete((Set<String>) any());
        
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            boolean existed = inMemoryStore.containsKey(key);
            inMemoryStore.remove(key);
            return existed ? 1L : 0L;
        }).when(template).delete((String) any());

        doAnswer(invocation -> valueOps).when(template).opsForValue();

        return template;
    }
}
