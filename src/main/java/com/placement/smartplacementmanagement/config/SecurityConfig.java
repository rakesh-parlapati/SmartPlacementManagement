
package com.placement.smartplacementmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // Disable CSRF because the application uses REST APIs
            .csrf(csrf -> csrf.disable())

            .userDetailsService(userDetailsService)

            .authorizeHttpRequests(auth -> auth

                // =========================
                // PUBLIC STATIC FILES
                // =========================
                .requestMatchers(
                    "/",
                    "/*.html",
                    "/CSS/**",
                    "/JS/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()

                // =========================
                // LOGIN / LOGOUT
                // =========================
                .requestMatchers(
                    "/auth/login",
                    "/auth/logout"
                ).permitAll()

                // =========================
                // PASSWORD RESET
                // =========================
                .requestMatchers(
                    "/api/password/**"
                ).permitAll()

                // Keep old password endpoints public too
                .requestMatchers(
                    "/auth/forgot-password",
                    "/auth/verify-otp",
                    "/auth/reset-password"
                ).permitAll()

                // =========================
                // STUDENT REGISTRATION
                // =========================
                .requestMatchers(
                    HttpMethod.POST,
                    "/students"
                ).permitAll()

                // =========================
                // COMPANIES
                // =========================
                .requestMatchers(
                    "/companies",
                    "/companies/**"
                ).authenticated()

                // =========================
                // STUDENTS
                // =========================
                .requestMatchers(
                    "/students",
                    "/students/**"
                ).authenticated()

                // =========================
                // APPLICATIONS
                // =========================
                .requestMatchers(
                    "/applications",
                    "/applications/**"
                ).authenticated()

                // =========================
                // EVERYTHING ELSE
                // =========================
                .anyRequest().authenticated()
            );

        return http.build();
    }
}

