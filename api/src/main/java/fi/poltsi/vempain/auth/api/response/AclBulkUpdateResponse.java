package fi.poltsi.vempain.auth.api.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** What a bulk ACL update changed and what it left alone */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Result of a bulk ACL update")
public class AclBulkUpdateResponse {

    @Schema(description = "IDs of the ACLs that were changed", example = "[41]")
    private List<Long> updatedAclIds;

    @Schema(description = "IDs of the ACLs that were left alone: unknown, or the caller holds no modify privilege on them", example = "[42]")
    private List<Long> skippedAclIds;
}
