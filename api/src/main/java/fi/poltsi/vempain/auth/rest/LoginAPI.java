package fi.poltsi.vempain.auth.rest;

import fi.poltsi.vempain.auth.api.Constants;
import fi.poltsi.vempain.auth.api.request.LoginRequest;
import fi.poltsi.vempain.auth.api.response.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@CrossOrigin(origins = "*", maxAge = 3600) // TODO Remove before going to production
@Tag(name = "Login", description = "REST endpoint to allow login")
public interface LoginAPI {
	@Operation(summary = "Authenticate user", description = "Verifies that the user is allowed to access the site", tags = "Login")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Login credentials", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "User authenticated",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = LoginResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@PostMapping(value = Constants.LOGIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest);
}
