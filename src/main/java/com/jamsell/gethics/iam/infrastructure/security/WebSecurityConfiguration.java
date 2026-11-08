package com.jamsell.gethics.iam.infrastructure.security;

import com.jamsell.gethics.iam.application.internal.outboundservices.tokens.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {

    private final TokenService tokenService;
    private final UserDetailsServiceImpl userDetailsService;
    private final JsonSecurityErrorHandler securityErrorHandler;

    public WebSecurityConfiguration(
            TokenService tokenService,
            UserDetailsServiceImpl userDetailsService,
            JsonSecurityErrorHandler securityErrorHandler
    ) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                                "/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/auth/**",
                                "/docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Transicion: los contextos de /api/v1 aun no estan migrados a identidad JWT (el dueno llega
                        // como ownerId UUID y el id de usuario de IAM es Long, sin correspondencia). Se mantienen
                        // publicos como antes de IAM; al migrar cada contexto, quitar esta regla y usar el principal.
                        .requestMatchers("/api/v1/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(
                        new BearerAuthorizationRequestFilter(tokenService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }
}
