package fi.poltsi.vempain.auth.testservice;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestServiceController {

	@GetMapping("/test-service/protected")
	public String protectedEndpoint() {
		return "protected";
	}

	@GetMapping("/test-service/resource/{aclId}")
	@PreAuthorize("@aclAuthorizationService.canRead(#p0)")
	public String readResource(@PathVariable("aclId") long aclId) {
		return "read";
	}

	@PostMapping("/test-service/resource/{aclId}")
	@PreAuthorize("@aclAuthorizationService.canCreate(#p0)")
	public String createResource(@PathVariable("aclId") long aclId) {
		return "create";
	}

	@PutMapping("/test-service/resource/{aclId}")
	@PreAuthorize("@aclAuthorizationService.canModify(#p0)")
	public String modifyResource(@PathVariable("aclId") long aclId) {
		return "modify";
	}

	@DeleteMapping("/test-service/resource/{aclId}")
	@PreAuthorize("@aclAuthorizationService.canDelete(#p0)")
	public String deleteResource(@PathVariable("aclId") long aclId) {
		return "delete";
	}
}
