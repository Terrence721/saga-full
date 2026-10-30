package com.example.saga.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

// CORS is handled here by a HandlerMapping-agnostic WebFilter so it applies uniformly
// to every path, including Gateway-routed ones. (There is no local auth controller anymore — login entrypoints were removed.)
//
// This bean replaces any RoutePredicateHandlerMapping-scoped globalcors config entirely.
@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(@Value("${FRONTEND_ORIGIN:http://localhost:5180}") String frontendOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name()));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
    }
}
