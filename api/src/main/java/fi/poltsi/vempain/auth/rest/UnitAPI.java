package fi.poltsi.vempain.auth.rest;

import fi.poltsi.vempain.auth.api.Constants;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.request.UnitRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
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
 * Unit (user group) management of the service's own user base. Units can contain users and other units (nested); a membership that
 * would make a unit contain itself through any chain is rejected with 400. Served by every service hosting {@code vempain-auth-core};
 * Read operations require authentication. Creating and updating units requires the modify privilege on the administrator ACL
 * ({@link Constants#ADMIN_ID}).
 */
@Tag(name = "UnitAPI", description = "Unit (user group) management")
public interface UnitAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/units";

	@Operation(summary = "Fetch list of all units", description = "Returns every unit with its member user and unit ids", tags = "UnitAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Returned a list of units",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UnitResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<UnitResponse>> getUnits();

	@Operation(summary = "Fetch a page of units", description = "Sorted by name (or id) and optionally filtered by name", tags = "UnitAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Page of units",
										content = @Content(schema = @Schema(implementation = PagedResponse.class),
	                                                       mediaType = MediaType.APPLICATION_JSON_VALUE)),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH + "/paged", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PagedResponse<UnitResponse>> getPagedUnits(@Valid @RequestBody PagedRequest request);

	@Operation(summary = "Fetch a specific unit by unit ID", description = "Returns the unit with its ACL and its member user and unit ids", tags = "UnitAPI")
	@Parameter(name = "unit_id", example = "12", description = "Unit ID to be fetched", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Got the details of a unit",
										content = {@Content(schema = @Schema(implementation = UnitResponse.class),
	                                                        mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "Unit not found", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{unit_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UnitResponse> findById(@PathVariable("unit_id") Long unitId);

	@Operation(summary = "Add a new unit", description = "Creates a unit with its ACL and its member users and units", tags = "UnitAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Unit created",
										content = {@Content(schema = @Schema(implementation = UnitResponse.class),
	                                                        mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request, e.g. a member that does not exist or a circular unit membership",
										content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UnitResponse> addUnit(@Valid @RequestBody UnitRequest unitRequest);

	@Operation(summary = "Update a specific unit", description = "Updates the unit, its ACL and its member users and units (user_ids and unit_ids "
																 + "replace the current members when given)", tags = "UnitAPI")
	@Parameter(name = "unit_id", example = "12", description = "Unit ID to be updated", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Unit updated",
										content = {@Content(schema = @Schema(implementation = UnitResponse.class),
	                                                        mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request, e.g. a member that does not exist or a circular unit membership",
										content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content),
						   @ApiResponse(responseCode = "404", description = "Unit not found", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping(value = MAIN_PATH + "/{unit_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UnitResponse> updateUnit(@PathVariable("unit_id") Long unitId, @Valid @RequestBody UnitRequest unitRequest);
}
