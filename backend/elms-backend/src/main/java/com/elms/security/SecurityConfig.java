package com.elms.security;

import com.elms.entity.User;
import com.elms.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserRepository userRepository;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, UserRepository userRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    Boolean.TRUE.equals(user.getEnabled()),
                    true,
                    true,
                    true,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/employees/**").hasRole("HR")
                        .requestMatchers(HttpMethod.GET, "/api/leave-types").hasAnyRole("EMPLOYEE", "MANAGER", "HR")
                        .requestMatchers(HttpMethod.GET, "/api/leave-types/{id}").hasAnyRole("EMPLOYEE", "MANAGER", "HR")
                        .requestMatchers(HttpMethod.POST, "/api/leave-types").hasRole("HR")
                        .requestMatchers(HttpMethod.PUT, "/api/leave-types/{id}").hasRole("HR")
                        .requestMatchers(HttpMethod.PUT, "/api/leave-types/{id}/activate").hasRole("HR")
                        .requestMatchers(HttpMethod.DELETE, "/api/leave-types/{id}").hasRole("HR")
                        .requestMatchers(HttpMethod.GET, "/api/holidays").hasAnyRole("EMPLOYEE", "MANAGER", "HR")
                        .requestMatchers(HttpMethod.GET, "/api/holidays/{id}").hasAnyRole("EMPLOYEE", "MANAGER", "HR")
                        .requestMatchers(HttpMethod.POST, "/api/holidays").hasRole("HR")
                        .requestMatchers(HttpMethod.PUT, "/api/holidays/{id}").hasRole("HR")
                        .requestMatchers(HttpMethod.DELETE, "/api/holidays/{id}").hasRole("HR")
                        .requestMatchers("/api/leave-balances/**").hasAnyRole("EMPLOYEE", "MANAGER", "HR")
                        .requestMatchers("/api/leave-requests/**").hasAnyRole("EMPLOYEE", "MANAGER", "HR")
                        .requestMatchers("/api/notifications/**").hasAnyRole("EMPLOYEE", "MANAGER", "HR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}
