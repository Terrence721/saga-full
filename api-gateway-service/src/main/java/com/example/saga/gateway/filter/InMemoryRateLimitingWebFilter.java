package com.example.saga.gateway.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class InMemoryRateLimitingWebFilter implements WebFilter, Ordered {

    private final int requestsPerMinute;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimitingWebFilter(@Value("${app.gateway.rate-limit.requests-per-minute:1000}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String client = exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();

        Instant now = Instant.now();
        Window window = windows.computeIfAbsent(client, key -> new Window(now));

        boolean allowed;
        synchronized (window) {
            if (window.start.plus(Duration.ofMinutes(1)).isBefore(now)) {
                window.start = now;
                window.count.set(0);
            }
            allowed = window.count.incrementAndGet() <= requestsPerMinute;
        }

        if (!allowed) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private static final class Window {
        private Instant start;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(Instant start) {
            this.start = start;
        }
    }

}
