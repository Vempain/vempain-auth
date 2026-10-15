package fi.poltsi.vempain.auth.api.response;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JSON contract of {@link AclResponse}: snake_case keys only, unknown keys ignored and {@link AclResponse#toRequest()} mirrors every field into an
 * {@link fi.poltsi.vempain.auth.api.request.AclRequest} without leaking into the JSON.
 */
@JsonTest
class AclResponseJTC {

    @Autowired
    private JacksonTester<AclResponse> jacksonTester;

    private static AclResponse userAcl() {
        return AclResponse.builder()
            .id(11L)
            .aclId(5L)
            .user(3L)
            .unit(null)
            .readPrivilege(true)
            .createPrivilege(false)
            .modifyPrivilege(true)
            .deletePrivilege(false)
            .build();
    }

    @Test
    void aclResponseIsWrittenWithSnakeCaseKeys() throws IOException {
        JsonContent<AclResponse> result = jacksonTester.write(userAcl());

        assertThat(result).extractingJsonPathNumberValue("@.id")
            .isEqualTo(11);
        assertThat(result).extractingJsonPathNumberValue("@.acl_id")
            .isEqualTo(5);
        assertThat(result).extractingJsonPathNumberValue("@.user")
            .isEqualTo(3);
        assertThat(result).hasEmptyJsonPathValue("@.unit");
        assertThat(result).extractingJsonPathBooleanValue("@.read_privilege")
            .isEqualTo(true);
        assertThat(result).extractingJsonPathBooleanValue("@.create_privilege")
            .isEqualTo(false);
        assertThat(result).extractingJsonPathBooleanValue("@.modify_privilege")
            .isEqualTo(true);
        assertThat(result).extractingJsonPathBooleanValue("@.delete_privilege")
            .isEqualTo(false);
        // camelCase keys and the request conversion must not appear in the JSON
        assertThat(result).doesNotHaveJsonPath("@.aclId")
            .doesNotHaveJsonPath("@.readPrivilege")
            .doesNotHaveJsonPath("@.request");
    }

    @Test
    void aclResponseIsReadFromSnakeCaseJsonIgnoringUnknownKeys() throws IOException {
        var json = """
                {"id": 11, "acl_id": 5, "user": null, "unit": 7, "read_privilege": true, "create_privilege": true,
                 "modify_privilege": false, "delete_privilege": false, "unknown_key": "ignored"}""";

        AclResponse response = jacksonTester.parseObject(json);

        assertEquals(11L, response.getId());
        assertEquals(5L, response.getAclId());
        assertNull(response.getUser());
        assertEquals(7L, response.getUnit());
        assertTrue(response.isReadPrivilege());
        assertTrue(response.isCreatePrivilege());
        assertFalse(response.isModifyPrivilege());
        assertFalse(response.isDeletePrivilege());
    }

    @Test
    void toRequestCopiesEveryField() {
        var request = userAcl().toRequest();

        assertEquals(11L, request.getId());
        assertEquals(5L, request.getAclId());
        assertEquals(3L, request.getUser());
        assertNull(request.getUnit());
        assertTrue(request.isReadPrivilege());
        assertFalse(request.isCreatePrivilege());
        assertTrue(request.isModifyPrivilege());
        assertFalse(request.isDeletePrivilege());
    }

    @Test
    void toRequestKeepsUnitAclsAsUnitRequests() {
        var request = AclResponse.builder()
            .id(12L)
            .aclId(5L)
            .user(null)
            .unit(7L)
            .deletePrivilege(true)
            .build()
            .toRequest();

        assertNull(request.getUser());
        assertEquals(7L, request.getUnit());
        assertFalse(request.isReadPrivilege());
        assertTrue(request.isDeletePrivilege());
    }

    @SpringBootConfiguration
    public static class TestConfig {
    }
}
