package com.luismunozse.reservalago.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(Customizer.withDefaults());
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers(
                        "/v3/api-docs/**",       // libre
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/docs"
                ).permitAll()
                .anyRequest().permitAll()
        );
        http.httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService users(
            @Value("${app.admin.username}") String username,
            @Value("${app.admin.password}") String rawPassword,
            PasswordEncoder encoder
    ) {
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(encoder.encode(rawPassword)) // hashea al arrancar
                        .roles("ADMIN")
                        .build()
        );
    }
}
