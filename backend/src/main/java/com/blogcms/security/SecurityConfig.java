package com.blogcms.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/sitemap.xml", "/robots.txt", "/rss.xml").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/health", "/actuator/health").permitAll()
                        // Server-rendered public site (Thymeleaf) + static admin panel shell.
                        // The admin panel's actual data calls go through /api/** below, which stays protected.
                        .requestMatchers("/", "/category/**", "/article/**", "/search", "/about", "/about/cv-request", "/gallery", "/press").permitAll()
                        .requestMatchers("/admin", "/admin/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/assets/**", "/webjars/**", "/favicon.ico").permitAll()
                        // The servlet error dispatch renders the public HTML error page (templates/error.html).
                        .requestMatchers("/error").permitAll()
                        // Everything under /api/** that was not explicitly permitted above needs a valid token.
                        .requestMatchers("/api/**").authenticated()
                        // Any other path is an unknown public URL: let it reach the DispatcherServlet so it
                        // resolves to a proper 404 page instead of a misleading 401 JSON body.
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
