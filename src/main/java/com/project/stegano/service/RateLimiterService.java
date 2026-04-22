package com.project.stegano.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public void checkLimit(String bucket, String key, int maxRequests, int windowSeconds) {
        long now = Instant.now().toEpochMilli();
        long cutoff = now - (windowSeconds * 1000L);
        String fullKey = bucket + ":" + key;

        Deque<Long> deque = buckets.computeIfAbsent(fullKey, ignored -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.pollFirst();
            }
            if (deque.size() >= maxRequests) {
                throw new IllegalArgumentException("Rate limit exceeded. Please slow down.");
            }
            deque.addLast(now);
        }
    }
}
