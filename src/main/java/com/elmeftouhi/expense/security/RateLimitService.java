package com.elmeftouhi.expense.security;

import com.elmeftouhi.expense.shared.exception.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final ConcurrentMap<String, Deque<Instant>> hitsByKey = new ConcurrentHashMap<>();

    public void assertWithinLimit(String key, int maxRequests, Duration window, String message) {
        Instant now = Instant.now();
        Deque<Instant> hits = hitsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (hits) {
            Instant cutoff = now.minus(window);
            while (!hits.isEmpty() && hits.peekFirst().isBefore(cutoff)) {
                hits.removeFirst();
            }

            if (hits.size() >= maxRequests) {
                throw new TooManyRequestsException(message);
            }

            hits.addLast(now);
        }
    }
}

