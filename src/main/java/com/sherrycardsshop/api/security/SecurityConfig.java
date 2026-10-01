package com.sherrycardsshop.api.security;

import com.sherrycardsshop.api.auth.entity.Rol;
import com.sherrycardsshop.api.auth.service.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class SecurityConfig {

    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

    static final RequestMatcher PUBLIC_REQUESTS = new OrRequestMatcher(
            PATHS.matcher(HttpMethod.OPTIONS, "/**"),
            PATHS.matcher(HttpMethod.GET, "/api/health"),
            PATHS.matcher(HttpMethod.GET, "/api/categories/**"),
            PATHS.matcher(HttpMethod.GET, "/api/novedades/**"),
            PATHS.matcher(HttpMethod.POST, "/api/auth/register"),
            PATHS.matcher(HttpMethod.POST, "/api/auth/login"),
            PATHS.matcher(HttpMethod.POST, "/api/auth/google"),
            PATHS.matcher(HttpMethod.POST, "/api/auth/refresh"),
            PATHS.matcher(HttpMethod.POST, "/api/auth/logout"),
            PATHS.matcher("/actuator/health/**"),
            PATHS.matcher("/actuator/info"),
            PATHS.matcher("/v3/api-docs/**"),
            PATHS.matcher("/swagger-ui/**"),
            PATHS.matcher("/swagger-ui.html"),
            PATHS.matcher("/error"));

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource,
                                            JsonSecurityErrorHandler errorHandler) throws Exception {
        return http
                // Sustituido por OriginValidationFilter: el token CSRF de Angular no se envía a otros orígenes.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(new OriginValidationFilter(corsConfigurationSource, errorHandler), CorsFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_REQUESTS).permitAll()
                        .requestMatchers(PATHS.matcher("/api/admin/**")).hasRole(Rol.ADMIN)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new CookieBearerTokenResolver(PUBLIC_REQUESTS))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .build();
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenService.ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
