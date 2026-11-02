package fi.poltsi.vempain.auth.api.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One grantee of a bulk ACL update: a user or a unit of the service's own user base and the privileges it gets on every ACL of the request. The same shape as
 * the rows of the ACL editors ({@code user}/{@code unit} + the four privileges), without the ids of persisted rows.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "A user or unit and the privileges it gets on every ACL of a bulk update")
public class AclBulkEntryRequest {

    @Schema(description = "ID of a user account; exactly one of user and unit is set", example = "12", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Positive
    private Long user;

    @Schema(description = "ID of a unit; exactly one of user and unit is set", example = "3", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Positive
    private Long unit;

    @Schema(description = "Privilege to create", example = "false")
    private boolean createPrivilege;

    @Schema(description = "Privilege to read", example = "true")
    private boolean readPrivilege;

    @Schema(description = "Privilege to modify", example = "false")
    private boolean modifyPrivilege;

    @Schema(description = "Privilege to delete", example = "false")
    private boolean deletePrivilege;

    @JsonIgnore
    @AssertTrue(message = "Exactly one of user and unit must be set")
    public boolean isUserOrUnit() {
        return (user == null) != (unit == null);
    }

    @JsonIgnore
    @AssertTrue(message = "At least one privilege must be granted")
    public boolean isAnyPrivilege() {
        return createPrivilege
               || readPrivilege
               || modifyPrivilege
               || deletePrivilege;
    }
}
