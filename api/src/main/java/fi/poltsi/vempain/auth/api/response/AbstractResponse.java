package fi.poltsi.vempain.auth.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;

@MappedSuperclass
@SuperBuilder
@Data
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Abstract response with base fields of metadata and ACL")
public class AbstractResponse {
	@Schema(description = "ID of the entity", example = "123")
	private Long id;
	@Schema(description = "List of ACL responses", example = "[{acl_id: 1, user: 1, unit: null, true, true, false, false}]")
	private List<AclResponse> acls;
	@Schema(description = "Whether the object should be locked from editing", example = "false")
	private boolean           locked;
	@Schema(description = "User ID of the creator", example = "1")
	private Long              creator;
	@Schema(description = "User ID of the last modifier", example = "10")
	private Long              modifier;
	@Schema(description = "When was the object created", example = "2022-05-13T16:03:44Z")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Instant           created;
	@Schema(description = "When was the object last modified", example = "2022-05-13T16:03:44Z")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Instant           modified;
}
