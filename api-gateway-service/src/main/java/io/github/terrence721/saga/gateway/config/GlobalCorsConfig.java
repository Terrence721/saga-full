package io.github.terrence721.saga.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

// Spring Cloud Gateway's own spring.cloud.gateway.server.webflux.globalcors property
// (no longer used here - see below) only wires into RoutePredicateHandlerMapping, the
// mapping that resolves this app's YAML-defined routes. It has no effect on a path served
// by a real local @RestController (AuthenticationController's /auth/login), since Spring
// dispatches that path to a completely different HandlerMapping with no CORS
// configuration of its own. Verified for real: a preflight to /auth/login was rejected
// with 403 while the identical origin worked fine on a Gateway-routed path (/orders) - a
// diagnostic WebFilter confirmed the gateway's own route was never even consulted for
// /auth/login, since the controller-dispatch mapping claimed the path first.
//
// This bean is a standard, HandlerMapping-agnostic WebFilter, so it applies uniformly
// regardless of which mapping ultimately serves a request - replacing globalcors
// entirely (removed from application.yaml) rather than running alongside it.
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
