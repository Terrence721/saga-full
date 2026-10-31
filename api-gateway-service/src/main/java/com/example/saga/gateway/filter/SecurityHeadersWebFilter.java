package com.example.saga.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersWebFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.addIfAbsent("X-Content-Type-Options", "nosniff");
        headers.addIfAbsent("X-Frame-Options", "DENY");
        headers.addIfAbsent("Referrer-Policy", "no-referrer");
        headers.addIfAbsent("X-XSS-Protection", "0");
        headers.addIfAbsent("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
