package fi.poltsi.vempain.auth.api.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fi.poltsi.vempain.auth.api.AclBulkUpdateModeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Grants the same entries on the ACLs of several resources at once: the ACL ids of the resources selected in a list (every ACL-bearing response carries its
 * rows with their {@code acl_id}), the mode, and the entries. Only the ACLs the caller may modify are changed; the others are reported as skipped.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Bulk update of the ACLs of several resources")
public class AclBulkUpdateRequest {

    @Schema(description = "IDs of the ACLs to change (the acl_id of the selected resources)", example = "[41, 42]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<@NotNull @Positive Long> aclIds;

    @Schema(description = "ADD keeps the existing rows and adds or updates the entries; REPLACE makes the entries the whole ACL", example = "ADD",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private AclBulkUpdateModeEnum mode;

    @Schema(description = "The users and units and their privileges", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Valid
    private List<AclBulkEntryRequest> entries;
}
