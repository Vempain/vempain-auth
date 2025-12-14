package fi.poltsi.vempain.auth.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Login token response")
public class LoginResponse {
	@Schema(description = "Type, is set to default", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
	private static final String TYPE = "Bearer";
	@Schema(description = "Token hash", example = "1234567890ABCDE", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private              String token;
	@Schema(description = "User ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@Min(value = 1)
	private              Long   id;

	@Schema(description = "Login name", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
	@Size(min = 2)
	private String login;

	@Schema(description = "Nick name", example = "System administrator", requiredMode = Schema.RequiredMode.REQUIRED)
	@Size(min = 2)
	private String nickname;

	@Schema(description = "User email address", example = "System administrator", requiredMode = Schema.RequiredMode.REQUIRED)
	@Email(message = "Email is not valid",
		   regexp = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\\\"(?:[\\\\x01-\\\\x08\\\\x0b\\\\x0c\\\\x0e-\\\\x1f\\\\x21\\\\x23-\\\\x5b\\\\x5d-\\\\x7f]|\\\\\\\\[\\\\x01-\\\\x09\\\\x0b\\\\x0c\\\\x0e-\\\\x7f])*\\\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\\\x01-\\\\x08\\\\x0b\\\\x0c\\\\x0e-\\\\x1f\\\\x21-\\\\x5a\\\\x53-\\\\x7f]|\\\\\\\\[\\\\x01-\\\\x09\\\\x0b\\\\x0c\\\\x0e-\\\\x7f])+)\\\\])")
	@NotEmpty(message = "Email cannot be empty")
	private String email;

	@Schema(description = "Token expiration time in ISO-8601 format", example = "2024-12-31T23:59:59Z", requiredMode = Schema.RequiredMode.REQUIRED)
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@NotNull
	private Instant expiresAt;

	@Schema(description = "List of units the user may belong to", example = "System administrator")
	private List<UnitResponse> units;
}
