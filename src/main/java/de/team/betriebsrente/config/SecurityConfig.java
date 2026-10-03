package de.team.betriebsrente.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.security.admin-user:fachbereich}")
    String adminUser;
    @Value("${app.security.admin-pass:Betriebsrente2024!}")
    String adminPassRaw;
    @Value("${app.security.team-user:team}")
    String teamUser;
    @Value("${app.security.team-pass:Willkommen2024!}")
    String teamPassRaw;

    @Bean
    public UserDetailsService users() {
        var admin = User.withUsername(adminUser)
                .password("{noop}" + adminPassRaw)
                .roles("ADMIN", "USER")
                .build();
        var team = User.withUsername(teamUser)
                .password("{noop}" + teamPassRaw)
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(admin, team);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().hasAnyRole("USER", "ADMIN"))
                .formLogin(form -> form
                        .loginPage("/login").permitAll()
                        .defaultSuccessUrl("/", true))
                .logout(l -> l.logoutSuccessUrl("/login?logout").permitAll())
                .httpBasic(b -> {
                });
        return http.build();
    }
}
