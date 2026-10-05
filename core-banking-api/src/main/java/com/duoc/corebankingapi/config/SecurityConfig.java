package com.duoc.corebankingapi.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;

/**
 * core-banking-api no valida JWT de usuario final (confia en que el BFF ya
 * lo hizo); su unica puerta es InternalApiKeyFilter sobre /internal/**.
 * Por eso no depende de spring-boot-starter-oauth2-resource-server, solo
 * de spring-boot-starter-security para este filtro y el PasswordEncoder.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/internal/cuentas/*/retiros")
                        .hasAuthority("SCOPE_core.write")
                        .requestMatchers("/internal/**").hasAuthority("SCOPE_core.read")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
