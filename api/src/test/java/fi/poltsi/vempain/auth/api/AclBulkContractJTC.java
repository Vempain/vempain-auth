package fi.poltsi.vempain.auth.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.auth.api.request.AclBulkEntryRequest;
import fi.poltsi.vempain.auth.api.request.AclBulkUpdateRequest;
import fi.poltsi.vempain.auth.api.response.AclBulkUpdateResponse;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JSON contract of the bulk ACL update consumed by the SPAs' list screens */
class AclBulkContractJTC {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requestIsSnakeCaseWithTheEditorRowShape() throws Exception {
        var request = objectMapper.readValue(
                "{\"acl_ids\": [41, 42], \"mode\": \"REPLACE\", \"entries\": [{\"user\": 7, \"unit\": null, "
                        + "\"create_privilege\": false, \"read_privilege\": true, \"modify_privilege\": true, \"delete_privilege\": false}]}",
                AclBulkUpdateRequest.class);
        assertEquals(List.of(41L, 42L), request.getAclIds());
        assertEquals(AclBulkUpdateModeEnum.REPLACE, request.getMode());
        var entry = request.getEntries()
            .getFirst();
        assertEquals(7L, entry.getUser());
        assertNull(entry.getUnit());
        assertTrue(entry.isReadPrivilege());
        assertTrue(entry.isModifyPrivilege());
        assertTrue(entry.isUserOrUnit());
        assertTrue(entry.isAnyPrivilege());
        assertEquals(Set.of("acl_ids", "mode", "entries"), new HashSet<>(objectMapper.readTree(objectMapper.writeValueAsString(request))
            .propertyNames()));
        assertEquals(Set.of("user", "unit", "create_privilege", "read_privilege", "modify_privilege", "delete_privilege"),
                new HashSet<>(objectMapper.readTree(objectMapper.writeValueAsString(entry))
                    .propertyNames()));
    }

    @Test
    void anEntryNamesExactlyOneGranteeAndGrantsSomething() {
        assertFalse(AclBulkEntryRequest.builder()
            .readPrivilege(true)
            .build()
            .isUserOrUnit());
        assertFalse(AclBulkEntryRequest.builder()
            .user(1L)
            .unit(2L)
            .readPrivilege(true)
            .build()
            .isUserOrUnit());
        assertFalse(AclBulkEntryRequest.builder()
            .unit(2L)
            .build()
            .isAnyPrivilege());
    }

    @Test
    void responseIsSnakeCase() throws Exception {
        var tree = objectMapper.readTree(objectMapper.writeValueAsString(AclBulkUpdateResponse.builder()
            .updatedAclIds(List.of(41L))
            .skippedAclIds(List.of(42L))
            .build()));
        assertEquals(Set.of("updated_acl_ids", "skipped_acl_ids"), new HashSet<>(tree.propertyNames()));
        assertEquals(41, tree.get("updated_acl_ids")
            .get(0)
            .asInt());
    }
}
