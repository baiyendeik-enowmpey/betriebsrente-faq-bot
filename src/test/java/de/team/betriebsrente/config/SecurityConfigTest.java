package de.team.betriebsrente.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class SecurityConfigTest {

    @Test
    void createsConfiguredAdminAndTeamUsersWithExpectedRoles() {
        SecurityConfig config = new SecurityConfig();
        config.adminUser = "admin-test";
        config.adminPassRaw = "admin-password";
        config.teamUser = "team-test";
        config.teamPassRaw = "team-password";

        var users = config.users();
        UserDetails admin = users.loadUserByUsername("admin-test");
        UserDetails team = users.loadUserByUsername("team-test");

        assertEquals("{noop}admin-password", admin.getPassword());
        assertTrue(admin.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertEquals("{noop}team-password", team.getPassword());
        assertTrue(team.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }
}
