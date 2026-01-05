package fi.poltsi.vempain.auth.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Unit request")
public class UnitRequest {
	@Schema(description = "Unit ID", example = "1")
	private long             id;
	@Schema(description = "Unit name", example = "users")
	private String           name;
	@Schema(description = "Unit description", example = "Normal users")
	private String           description;
	@Schema(description = "List of ACL responses", example = "{acl_id: 1, user: 1, unit: null, \"YES\", \"YES\", \"NO\", \"NO\"}")
	private List<AclRequest> acls;
	@Schema(description = "Whether the object should be locked from editing", example = "false")
	private boolean          locked;
	@Schema(description = "User ID of the creator", example = "1")
	private Long             creator;
	@Schema(description = "User ID of the last modifier", example = "10")
	private Long             modifier;
	@Schema(description = "When was the object created", example = "2022-05-13T16:03:44Z")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Instant          created;
	@Schema(description = "When was the object last modified", example = "2022-05-13T16:03:44Z")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Instant          modified;
}
