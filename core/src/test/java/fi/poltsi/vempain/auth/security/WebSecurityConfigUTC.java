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

    @Test
    void corsExposesTheRenewedSessionHeadersToTheBrowser() {
        var config = new WebSecurityConfig(null, null, null) {

            @Override
            protected void configureApplicationAuthorization(ApplicationAuthorizationConfigurer authorization) {}
        };
        org.springframework.test.util.ReflectionTestUtils.setField(config, "allowedOrigins", java.util.List.of("https://admin.example.org"));
        org.springframework.test.util.ReflectionTestUtils.setField(config, "maxAge", 3600L);
        org.springframework.test.util.ReflectionTestUtils.setField(config, "corsPattern", "/**");
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/content-management/pages");

        var cors = config.corsConfigurationSource()
            .getCorsConfiguration(request);

        org.junit.jupiter.api.Assertions.assertNotNull(cors);
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(fi.poltsi.vempain.auth.api.Constants.RENEWED_TOKEN_HEADER,
                fi.poltsi.vempain.auth.api.Constants.RENEWED_TOKEN_EXPIRES_AT_HEADER), cors.getExposedHeaders());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("https://admin.example.org"), cors.getAllowedOrigins());
    }
}
