package de.team.betriebsrente.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

class SecurityConfigTest {

    @Test
    void usesPostgresBackedUserDetailsManager() {
        SecurityConfig config = new SecurityConfig();
        assertInstanceOf(JdbcUserDetailsManager.class, config.users(mock(DataSource.class)));
    }
}
