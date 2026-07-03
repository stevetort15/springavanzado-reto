package com.ejemplo.facturacion.config;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ejemplo.facturacion.security.JwtFilter;

/**
 * Configuración de seguridad con JWT:
 *  - /autenticar y /actuator/** se pueden invocar sin token.
 *  - Cualquier otro request (v1 y v2) requiere un token JWT válido.
 *  - La API es stateless (sin sesión) y el JwtFilter valida el token
 *    antes del filtro de autenticación por usuario/contraseña.
 */
@Configuration
public class GestionUsuariosConfig {
    @Bean
    public PasswordEncoder passwordEncoder() throws NoSuchAlgorithmException {
        SecureRandom s = SecureRandom.getInstanceStrong();
        return new BCryptPasswordEncoder(4, s);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter)
            throws Exception {

        // API REST sin navegador: se deshabilita la protección CSRF
        http.csrf(csrf -> csrf.disable());

        // /autenticar y /actuator sin token; todo lo demás autenticado
        http.authorizeHttpRequests(
                c -> c.requestMatchers("/autenticar").permitAll()
                      .requestMatchers("/actuator/**").permitAll()
                      .anyRequest().authenticated());

        // Sin estado entre llamadas (no se crean sesiones)
        http.sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // El filtro JWT valida el token en cada request
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
