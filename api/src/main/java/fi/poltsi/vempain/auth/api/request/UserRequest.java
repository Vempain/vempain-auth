package fi.poltsi.vempain.auth.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.poltsi.vempain.auth.api.PrivacyType;
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
@Schema(description = "User request")
public class UserRequest {
	@Schema(description = "User ID", example = "1")
	private Long             id;
	@Schema(description = "Private user", example = "true")
	private boolean          privateUser;
	@Schema(description = "Full name", example = "Arnold Dunkelswetter")
	private String           name;
	@Schema(description = "Nick name", example = "Ahnold")
	private String           nick;
	@Schema(description = "Login name", example = "arnold")
	private String           loginName;
	@Schema(description = "Password", example = "verySecretPassword#1")
	private String      password;
	@Schema(description = "Privacy type", example = "1")
	private PrivacyType privacyType;
	@Schema(description = "Email address", example = "someone@noaddress.com")
	private String      email;
	@Schema(description = "Street", example = "Wallace st 12")
	private String           street;
	@Schema(description = "Post box number", example = "01234")
	private String           pob;
	@Schema(description = "Birthday", example = "2022-05-13T16:03:44Z")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Instant          birthday;
	@Schema(description = "Description", example = "Sysops in Internal IT")
	private String           description;
	@Schema(description = "List of ACL requests", example = "{acl_id: 1, user: 1, unit: null, \"YES\", \"YES\", \"NO\", \"NO\"}")
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
