package fi.poltsi.vempain.auth.rest;

import fi.poltsi.vempain.auth.api.Constants;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Read-only view of the ACL rows of the service's own user base, for the permissions overview. Administrator access is required.
 */
@Tag(name = "AclAPI", description = "ACL rows of the service")
public interface AclAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/acls";

	@Operation(summary = "Fetch list of all ACL rows", description = "Returns every ACL row", tags = "AclAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Got list of ACLs",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = AclResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<AclResponse>> getAllAcl();

	@Operation(summary = "Fetch the rows of one ACL", description = "Returns the rows sharing the given acl_id", tags = "AclAPI")
	@Parameter(name = "acl_id", example = "123", description = "ID of the ACL whose rows to return", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Got list of ACL rows",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = AclResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No such ACL", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{acl_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<AclResponse>> getAcl(@PathVariable(name = "acl_id") Long aclId);
}
