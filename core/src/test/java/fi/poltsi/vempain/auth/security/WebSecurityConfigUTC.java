package fi.poltsi.vempain.auth.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The defaults a host application inherits from {@link WebSecurityConfig} when it overrides nothing but the application rules.
 */
class WebSecurityConfigUTC {

    @Test
    void hostApplicationsAddNoAuthenticationFiltersByDefault() {
        var config = new WebSecurityConfig(null, null, null) {

            @Override
            protected void configureApplicationAuthorization(ApplicationAuthorizationConfigurer authorization) {}
        };

        assertTrue(config.additionalAuthenticationFilters()
            .isEmpty());
    }
}
