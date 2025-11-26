package fi.poltsi.vempain.auth.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@Jacksonized
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Item depicting a user item")
public class UserResponse extends AbstractResponse {
	@Schema(description = "Private user", example = "true")
	private boolean       privateUser;
	@Schema(description = "Full name", example = "Arnold Dunkelswetter")
	private String        name;
	@Schema(description = "Nick name", example = "Ahnold")
	private String        nick;
	@Schema(description = "Login name", example = "arnold")
	private String      loginName;
	@Schema(description = "Privacy type", example = "1")
	private PrivacyType privacyType;
	@Schema(description = "Email address", example = "someone@noaddress.com")
	private String      email;
	@Schema(description = "Street", example = "Wallace st 12")
	private String        street;
	@Schema(description = "Post box number", example = "01234")
	private String        pob;
	@Schema(description = "Birthday", example = "2022-05-13T16:03:44Z")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Instant       birthday;
	@Schema(description = "Description", example = "Sysops in Internal IT")
	private String        description;
	@Schema(description = "Status", example = "ACTIVE")
	private AccountStatus status;
}
