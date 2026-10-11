package de.team.betriebsrente.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final JdbcTemplate jdbcTemplate;
    private final UserDetailsManager users;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(JdbcTemplate jdbcTemplate, UserDetailsManager users, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Account> listUsers() {
        return jdbcTemplate.query("""
                SELECT u.username, u.enabled, a.authority
                FROM users u JOIN authorities a ON a.username = u.username
                ORDER BY u.username, a.authority
                """, rs -> {
            List<Account> accounts = new ArrayList<>();
            String currentUsername = null;
            boolean currentEnabled = true;
            boolean currentAdmin = false;
            while (rs.next()) {
                String username = rs.getString("username");
                if (currentUsername != null && !currentUsername.equals(username)) {
                    accounts.add(new Account(currentUsername, currentEnabled, currentAdmin));
                    currentAdmin = false;
                }
                currentUsername = username;
                currentEnabled = rs.getBoolean("enabled");
                currentAdmin |= "ROLE_ADMIN".equals(rs.getString("authority"));
            }
            if (currentUsername != null) {
                accounts.add(new Account(currentUsername, currentEnabled, currentAdmin));
            }
            return accounts;
        });
    }

    @Transactional
    public void createUser(String username, String rawPassword, String role) {
        String normalizedUsername = validateUsername(username);
        validatePassword(rawPassword);
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Rolle muss USER oder ADMIN sein.");
        }
        if (users.userExists(normalizedUsername)) {
            throw new IllegalArgumentException("Dieser Benutzername ist bereits vergeben.");
        }
        var builder = User.withUsername(normalizedUsername)
                .password(passwordEncoder.encode(rawPassword));
        if ("ADMIN".equals(role)) {
            builder.roles("ADMIN", "USER");
        } else {
            builder.roles(role);
        }
        users.createUser(builder.build());
    }

    @Transactional
    public void resetPassword(String username, String rawPassword) {
        validatePassword(rawPassword);
        UserDetails existing = loadUser(username);
        UserDetails replacement = User.withUserDetails(existing)
                .password(passwordEncoder.encode(rawPassword))
                .build();
        users.updateUser(replacement);
    }

    @Transactional
    public void deleteUser(String username, String actingUsername) {
        UserDetails existing = loadUser(username);
        if (existing.getUsername().equals(actingUsername)) {
            throw new IllegalArgumentException("Sie können Ihr eigenes Konto nicht löschen.");
        }
        boolean isAdmin = existing.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (isAdmin) {
            Integer adminCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT username) FROM authorities WHERE authority = 'ROLE_ADMIN'", Integer.class);
            if (adminCount != null && adminCount <= 1) {
                throw new IllegalArgumentException("Der letzte Administrator kann nicht gelöscht werden.");
            }
        }
        users.deleteUser(existing.getUsername());
    }

    private UserDetails loadUser(String username) {
        try {
            return users.loadUserByUsername(username);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Benutzer nicht gefunden.");
        }
    }

    private String validateUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (!normalized.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new IllegalArgumentException("Benutzername: 3–50 Zeichen, nur Buchstaben, Zahlen, Punkt, _ und -.");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Das Passwort muss mindestens 12 Zeichen lang sein.");
        }
    }

    public record Account(String username, boolean enabled, boolean admin) {
    }
}
