package fi.poltsi.vempain.auth.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Almost like {AclResponse} except that the user and unit fields are only the ID
 */
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Request depicting an ACL permission for either an user or unit")
public class AclRequest {
	@Schema(description = "Permission ID, not used for new objects", example = "1")
	Long id;
	@Schema(description = "ACL ID, not used for new objects", example = "1")
	Long aclId;
	@Schema(description = "Alternative user information, must be null if unit is set", example = "{Long}")
	Long  user;
	@Schema(description = "Alternative unit information, must be null if user is set", example = "{Long}")
	Long  unit;
	@Schema(description = "Privilege to create", example = "true")
	boolean createPrivilege;
	@Schema(description = "Privilege to read", example = "true")
	boolean readPrivilege;
	@Schema(description = "Privilege to modify", example = "true")
	boolean modifyPrivilege;
	@Schema(description = "Privilege to delete", example = "true")
	boolean deletePrivilege;
}
