package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        final java.util.Map<String, UserCredentials> users = new java.util.HashMap<>();

        // Administrador con acceso total
        users.put("Milagros", new UserCredentials("{noop}123456789", new String[]{"ADMIN", "USER"}));

        // Empleado 1
        users.put("Julio", new UserCredentials("{noop}123", new String[]{"USER"}));

        // Empleado 2
        users.put("German", new UserCredentials("{noop}123", new String[]{"USER"}));

        // Empleado 3
        users.put("Santiago", new UserCredentials("{noop}123", new String[]{"USER"}));

        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                UserCredentials creds = users.get(username);
                if (creds == null) {
                    throw new UsernameNotFoundException("Usuario no encontrado: " + username);
                }
                // Retornar una nueva instancia fresca cada vez para evitar la alteración por borrado de credenciales
                return User.withUsername(username)
                        .password(creds.getPassword())
                        .roles(creds.getRoles())
                        .build();
            }
        };
    }

    // Estructura interna inmutable para guardar contraseñas y roles
    private static class UserCredentials {
        private final String password;
        private final String[] roles;

        public UserCredentials(String password, String[] roles) {
            this.password = password;
            this.roles = roles;
        }

        public String getPassword() {
            return password;
        }

        public String[] getRoles() {
            return roles;
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.none()) // Evita cambiar el ID de cookie en HTTP local
            )
            .authorizeHttpRequests(auth -> auth
                // Recursos públicos y página de login
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                // Todos los usuarios autenticados (USER y ADMIN) tienen permisos completos
                .requestMatchers("/marcaciones/nuevo").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/marcaciones/editar/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/marcaciones/guardar").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/marcaciones/eliminar/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/marcaciones/limpiar").hasAnyRole("USER", "ADMIN")

                // USER y ADMIN pueden ver, filtrar y cargar archivos
                .requestMatchers("/", "/dashboard", "/marcaciones", "/marcaciones/upload", "/marcaciones/ejemplo").hasAnyRole("USER", "ADMIN")

                // Cualquier otra petición requiere autenticación
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?loggedout")
                .invalidateHttpSession(true) // Destruye la sesión en el servidor para permitir cambiar de usuario
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/login?denied")
            );

        return http.build();
    }
}
