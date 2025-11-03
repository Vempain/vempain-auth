package fi.poltsi.vempain.auth.api.request;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@JsonTest
class AclRequestJTC {
	@Autowired
	private JacksonTester<AclRequest> jacksonTester;

	@Test
	void aclRequestTest() throws IOException {
		AclRequest aclRequest = AclRequest.builder()
										  .id(1L)
										  .aclId(1L)
										  .user(1L)
										  .unit(null)
										  .readPrivilege(true)
										  .modifyPrivilege(false)
										  .createPrivilege(true)
										  .deletePrivilege(false)
										  .build();
		JsonContent<AclRequest> result = this.jacksonTester.write(aclRequest);

		assertThat(result).hasJsonPathNumberValue("@.id")
						  .hasJsonPathNumberValue("@.acl_id")
						  .hasJsonPathNumberValue("@.user")
						  .hasEmptyJsonPathValue("@.unit_id")
						  .hasJsonPathBooleanValue("@.read_privilege")
						  .hasJsonPathBooleanValue("@.modify_privilege")
						  .hasJsonPathBooleanValue("@.create_privilege")
						  .hasJsonPathBooleanValue("@.delete_privilege");

		assertThat(result).extractingJsonPathBooleanValue("@.read_privilege")
						  .isEqualTo(true);
		assertThat(result).extractingJsonPathBooleanValue("@.modify_privilege")
						  .isEqualTo(false);
		assertThat(result).extractingJsonPathBooleanValue("@.create_privilege")
						  .isEqualTo(true);
		assertThat(result).extractingJsonPathBooleanValue("@.delete_privilege")
						  .isEqualTo(false);
	}

	@SpringBootConfiguration
	public static class TestConfig {
	}
}
