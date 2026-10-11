package de.team.betriebsrente.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;

@Component
public class BootstrapUsers implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final UserDetailsManager users;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String teamUsername;
    private final String teamPassword;

    public BootstrapUsers(JdbcTemplate jdbcTemplate, UserDetailsManager users, PasswordEncoder passwordEncoder,
            @Value("${app.security.bootstrap-admin.username:fachbereich}") String adminUsername,
            @Value("${app.security.bootstrap-admin.password:}") String adminPassword,
            @Value("${app.security.bootstrap-team.username:team}") String teamUsername,
            @Value("${app.security.bootstrap-team.password:}") String teamPassword) {
        this.jdbcTemplate = jdbcTemplate;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.teamUsername = teamUsername;
        this.teamPassword = teamPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (userCount != null && userCount > 0) {
            return;
        }

        createBootstrapUser(adminUsername, adminPassword, "ADMIN", true);
        createBootstrapUser(teamUsername, teamPassword, "USER", false);
    }

    private void createBootstrapUser(String username, String rawPassword, String role, boolean admin) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalStateException("Set a username and password for the initial " + role + " account.");
        }
        var builder = User.withUsername(username.trim())
                .password(passwordEncoder.encode(rawPassword));
        if (admin) {
            builder.roles("ADMIN", "USER");
        } else {
            builder.roles(role);
        }
        users.createUser(builder.build());
    }
}
