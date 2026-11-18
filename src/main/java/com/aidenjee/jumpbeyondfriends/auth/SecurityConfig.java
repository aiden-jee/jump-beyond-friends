package com.aidenjee.jumpbeyondfriends.auth;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration 
@EnableWebSecurity 
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http, 
        AuthService authService
    ) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth
                            // Csrf endpoint accessible to all
                            .requestMatchers("/api/csrf").permitAll()

                            // Google auth users can access auth endpoints
                            .requestMatchers("/api/me").authenticated()

                            // Only enrolled players can access business logic APIs
                            .requestMatchers("/api/**")
                            .access((authentication, context) -> 
                                new AuthorizationDecision(authService.isRegistered(authentication.get()))
                            )       

                            // allow all other requests including front end routes
                            .anyRequest().permitAll()
                    )
                    .exceptionHandling(ex -> 
                        ex.defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            PathPatternRequestMatcher.withDefaults()
                                    .matcher("/api/**")
                        )
                    )
                    .oauth2Login(Customizer.withDefaults())
                    .csrf(csrf -> csrf.spa())
                    .build();
    }
}
