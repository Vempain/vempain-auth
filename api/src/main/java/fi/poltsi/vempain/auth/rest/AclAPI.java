package fi.poltsi.vempain.auth.rest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import fi.poltsi.vempain.auth.api.Constants;
import fi.poltsi.vempain.auth.api.request.AclBulkUpdateRequest;
import fi.poltsi.vempain.auth.api.response.AclBulkUpdateResponse;
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

/**
 * The ACL rows of the service's own user base: the read-only overview (administrator access) and the bulk update of the ACLs of selected resources (the modify
 * privilege on each ACL, i.e. resource-based like every Vempain authorization).
 */
@Tag(name = "AclAPI", description = "ACL rows of the service")
public interface AclAPI {

    String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/acls";

    @Operation(summary = "Fetch list of all ACL rows", description = "Returns every ACL row", tags = "AclAPI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Got list of ACLs",
                content = {
                    @Content(array = @ArraySchema(schema = @Schema(implementation = AclResponse.class)), mediaType = MediaType.APPLICATION_JSON_VALUE) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<AclResponse>> getAllAcl();

    @Operation(summary = "Fetch the rows of one ACL", description = "Returns the rows sharing the given acl_id", tags = "AclAPI")
    @Parameter(name = "acl_id", example = "123", description = "ID of the ACL whose rows to return", required = true)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Got list of ACL rows",
                content = {
                    @Content(array = @ArraySchema(schema = @Schema(implementation = AclResponse.class)), mediaType = MediaType.APPLICATION_JSON_VALUE) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content),
        @ApiResponse(responseCode = "404", description = "No such ACL", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH + "/{acl_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<AclResponse>> getAcl(@PathVariable(name = "acl_id") Long aclId);

    @Operation(summary = "Add or replace the rows of several ACLs at once",
            description = "Grants the entries on every ACL of the request: ADD adds the entries next to the existing rows (an entry for a user or unit "
                    + "that already has a row replaces that row's privileges), REPLACE makes the entries the whole ACL. Only the ACLs that grant the "
                    + "caller the modify privilege are changed; unknown ACLs and ACLs the caller may not modify are reported as skipped. Used by the "
                    + "lists of ACL-bearing resources to set the permissions of the selected rows.",
            tags = "AclAPI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The changed and the skipped ACLs",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AclBulkUpdateResponse.class)) }),
        @ApiResponse(responseCode = "400",
                description = "Invalid request: no ACLs or entries, an entry without a user or unit, with both, without a "
                        + "privilege, or naming an unknown user or unit",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = MAIN_PATH + "/bulk", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AclBulkUpdateResponse> bulkUpdate(@Valid @RequestBody AclBulkUpdateRequest request);
}
