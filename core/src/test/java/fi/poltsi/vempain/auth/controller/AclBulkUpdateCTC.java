package fi.poltsi.vempain.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The bulk ACL update over HTTP against real ACL rows: ADD keeps and merges, REPLACE rewrites, an ACL the caller may not modify is skipped, and an unknown user
 * is rejected with 400. The caller is never the administrator guard's business: a plain user changes the ACLs that grant it modify.
 */
@SpringBootTest(classes = TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AclBulkUpdateCTC extends IntegrationTestSetup {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtUtils jwtUtils;

    private RequestPostProcessor as(long userId) {
        var account = userAccountRepository.findById(userId)
            .orElseThrow();
        return user(UserDetailsImpl.build(account));
    }

    @Test
    void addMergesIntoTheModifiableAclsAndSkipsTheRest() throws Exception {
        var owner = testITCTools.generateUser();
        var grantee = testITCTools.generateUser();
        // the owner holds modify on the first ACL, only read on the second; the third does not exist
        var mine = testITCTools.generateAcl(owner, null, true, true, true, true);
        var readOnly = testITCTools.generateAcl(owner, null, true, false, false, false);
        var body = "{\"acl_ids\": ["
                + mine
                + ", "
                + readOnly
                + ", 987654321], \"mode\": \"ADD\", \"entries\": [{\"user\": "
                + grantee
                + ", \"read_privilege\": true, \"modify_privilege\": true}, {\"user\": "
                + owner
                + ", \"read_privilege\": true}]}";

        mockMvc.perform(post("/content-management/acls/bulk").with(as(owner))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.updated_acl_ids", contains(mine.intValue())))
            .andExpect(jsonPath("$.skipped_acl_ids", contains(readOnly.intValue(), 987654321)));

        var rows = aclService.findAclByAclId(mine);
        assertEquals(2, rows.size());
        var ownerRow = rows.stream()
            .filter(row -> owner.equals(row.getUserId()))
            .findFirst()
            .orElseThrow();
        // ADD replaced the owner's privileges with the entry's (read only) and added the grantee
        assertTrue(ownerRow.isReadPrivilege());
        assertTrue(!ownerRow.isModifyPrivilege());
        var granteeRow = rows.stream()
            .filter(row -> grantee.equals(row.getUserId()))
            .findFirst()
            .orElseThrow();
        assertTrue(granteeRow.isModifyPrivilege());
        assertEquals(1, aclService.findAclByAclId(readOnly)
            .size(), "the skipped ACL is untouched");
    }

    @Test
    void replaceRewritesTheAclAndUnknownGranteesAreRejected() throws Exception {
        var owner = testITCTools.generateUser();
        var unitId = testITCTools.generateUnit();
        var mine = testITCTools.generateAcl(owner, null, true, true, true, true);

        mockMvc.perform(post("/content-management/acls/bulk").with(as(owner))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"acl_ids\": [" + mine + "], \"mode\": \"REPLACE\", \"entries\": [{\"unit\": " + unitId + ", \"read_privilege\": true}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.updated_acl_ids", hasSize(1)));

        var rows = aclService.findAclByAclId(mine);
        assertEquals(1, rows.size(), "the owner's row went with the replacement");
        assertEquals(unitId, rows.getFirst()
            .getUnitId());

        // the owner no longer holds modify: a further update of the same ACL is skipped
        mockMvc.perform(post("/content-management/acls/bulk").with(as(owner))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"acl_ids\": [" + mine + "], \"mode\": \"ADD\", \"entries\": [{\"user\": " + owner + ", \"read_privilege\": true}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.updated_acl_ids", hasSize(0)))
            .andExpect(jsonPath("$.skipped_acl_ids", contains(mine.intValue())));

        // validation: an entry without a grantee, and one naming an unknown user
        var other = testITCTools.generateAcl(owner, null, true, true, true, true);
        mockMvc.perform(post("/content-management/acls/bulk").with(as(owner))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"acl_ids\": [" + other + "], \"mode\": \"ADD\", \"entries\": [{\"read_privilege\": true}]}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/content-management/acls/bulk").with(as(owner))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"acl_ids\": [" + other + "], \"mode\": \"ADD\", \"entries\": [{\"user\": 987654321, \"read_privilege\": true}]}"))
            .andExpect(status().isBadRequest());
        assertEquals(1, aclService.findAclByAclId(other)
            .size(), "a rejected update changes nothing");
    }
}
