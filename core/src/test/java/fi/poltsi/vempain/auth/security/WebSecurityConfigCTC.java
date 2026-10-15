package fi.poltsi.vempain.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import fi.poltsi.vempain.auth.configuration.TestWebSecurityConfig;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The shared {@link WebSecurityConfig} outside the {@code local} profile: the documentation and actuator paths are denied to everybody (anonymous callers get
 * the JWT entry point's 401, authenticated ones the access denied handler's 403), the application rules a host registers through both {@code authenticated}
 * overloads require authentication, and the additional authentication filters a host adds run inside the security filter chain.
 */
@SpringBootTest(classes = TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class WebSecurityConfigCTC extends IntegrationTestSetup {

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor admin() {
        var account = userAccountRepository.findById(ADMIN_ID)
            .orElseThrow();
        return user(UserDetailsImpl.build(account));
    }

    @Test
    void documentationAndActuatorPathsAreDeniedToAnonymousCallers() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs/public"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void documentationAndActuatorPathsAreForbiddenToAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/swagger-ui.html").with(admin()))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/v3/api-docs/public").with(admin()))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/prometheus").with(admin()))
            .andExpect(status().isForbidden());
    }

    @Test
    void applicationRulesRequireAuthentication() throws Exception {
        mockMvc.perform(get(TestWebSecurityConfig.PATTERN_RULE_PATH + "/resource"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post(TestWebSecurityConfig.METHOD_RULE_PATH + "/resource"))
            .andExpect(status().isUnauthorized());
        // An authenticated user passes the rules; the paths have no handler, so the request ends with 404 instead of a security status
        mockMvc.perform(get(TestWebSecurityConfig.PATTERN_RULE_PATH + "/resource").with(admin()))
            .andExpect(status().isNotFound());
        mockMvc.perform(post(TestWebSecurityConfig.METHOD_RULE_PATH + "/resource").with(admin()))
            .andExpect(status().isNotFound());
    }

    @Test
    void additionalAuthenticationFiltersRunInsideTheSecurityFilterChain() throws Exception {
        mockMvc.perform(get("/content-management/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(TestWebSecurityConfig.ADDITIONAL_FILTER_HEADER, "ran"));
        mockMvc.perform(get("/content-management/users").with(admin()))
            .andExpect(status().isOk())
            .andExpect(header().string(TestWebSecurityConfig.ADDITIONAL_FILTER_HEADER, "ran"));
    }

    @Test
    void healthAndErrorPathsStayOpen() throws Exception {
        // No actuator on the classpath: the permitted health path reaches the dispatcher and ends with 404, not 401
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/error"))
            .andExpect(status().is(not(401)));
    }
}
