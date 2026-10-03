package fi.poltsi.vempain.auth.testservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestServiceController {

	@GetMapping("/test-service/protected")
	public String protectedEndpoint() {
		return "protected";
	}

	@PostMapping("/test-service/admin")
	public String adminEndpoint() {
		return "admin";
	}
}
