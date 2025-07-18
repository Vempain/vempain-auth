package fi.poltsi.vempain.auth.api.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Item depicting an ACL permission for either an user or unit")
public class AclResponse implements Serializable {
	@Schema(description = "Permission ID", example = "1")
	@NotBlank
	Long    id;
	@Schema(description = "ACL ID", example = "1")
	@NotBlank
	Long    aclId;
	@Schema(description = "User ID of the privilege, must be null if unit is set", example = "1")
	Long    user;
	@Schema(description = "Unit ID of the privilege, must be null if user is set", example = "1")
	Long    unit;
	@Schema(description = "Privilege to create", example = "true")
	boolean createPrivilege;
	@Schema(description = "Privilege to read", example = "true")
	boolean readPrivilege;
	@Schema(description = "Privilege to modify", example = "true")
	boolean modifyPrivilege;
	@Schema(description = "Privilege to delete", example = "true")
	boolean deletePrivilege;

	@JsonIgnore
	public AclRequest toRequest() {
		return AclRequest.builder()
						 .id(this.id)
						 .aclId(this.aclId)
						 .user(this.user)
						 .unit(this.unit)
						 .createPrivilege(this.createPrivilege)
						 .readPrivilege(this.readPrivilege)
						 .modifyPrivilege(this.modifyPrivilege)
						 .deletePrivilege(this.deletePrivilege)
						 .build();
	}
}
