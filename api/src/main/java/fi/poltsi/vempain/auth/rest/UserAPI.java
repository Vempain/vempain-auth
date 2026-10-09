package fi.poltsi.vempain.auth.rest;

import fi.poltsi.vempain.auth.api.Constants;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.request.UserRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * User account management of the service's own user base. Every service that hosts {@code vempain-auth-core} serves this API; the
 * caller needs the modify privilege on the administrator ACL ({@link Constants#ADMIN_ID}).
 */
@Tag(name = "UserAPI", description = "User account management")
public interface UserAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/users";

	@Operation(summary = "Fetch list of all users", description = "Returns a list of all user accounts", tags = "UserAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Returned a list of users",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<UserResponse>> getUsers();

	@Operation(summary = "Fetch a page of users", description = "Sorted by name (or id) and optionally filtered by name or login name", tags = "UserAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Page of users",
										content = @Content(schema = @Schema(implementation = PagedResponse.class),
	                                                       mediaType = MediaType.APPLICATION_JSON_VALUE)),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH + "/paged", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PagedResponse<UserResponse>> getPagedUsers(@Valid @RequestBody PagedRequest request);

	@Operation(summary = "Fetch a specific user by user ID", description = "Returns the user with its ACL and unit memberships", tags = "UserAPI")
	@Parameter(name = "user_id", example = "12", description = "User ID to be fetched", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Got the details of a user",
										content = {@Content(schema = @Schema(implementation = UserResponse.class),
	                                                        mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content),
						   @ApiResponse(responseCode = "404", description = "User not found", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{user_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UserResponse> findById(@PathVariable("user_id") Long userId);

	@Operation(summary = "Add a new user", description = "Creates a user account with its ACL and optional unit memberships", tags = "UserAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "User account created",
										content = {@Content(schema = @Schema(implementation = UserResponse.class),
	                                                        mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UserResponse> addUser(@Valid @RequestBody UserRequest userRequest);

	@Operation(summary = "Update a specific user",
	           description = "Updates the account, its ACL and, when unit_ids is given, its unit memberships",
	           tags = "UserAPI")
	@Parameter(name = "user_id", example = "12", description = "User ID to be updated", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "User details updated",
										content = {@Content(schema = @Schema(implementation = UserResponse.class),
	                                                        mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content),
						   @ApiResponse(responseCode = "404", description = "User not found", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping(value = MAIN_PATH + "/{user_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UserResponse> updateUser(@PathVariable("user_id") Long userId, @Valid @RequestBody UserRequest userRequest);
}
