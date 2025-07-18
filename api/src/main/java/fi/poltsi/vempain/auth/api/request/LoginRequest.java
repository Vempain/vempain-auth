package fi.poltsi.vempain.auth.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Login request")
public class LoginRequest {
	@Schema(description = "Login name", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String login;
	@Schema(description = "Login password", example = "qwerty", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String password;
}
