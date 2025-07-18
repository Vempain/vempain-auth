package fi.poltsi.vempain.auth.tools;

import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class AuthTools {
	public static boolean isUserIdCurrentUser(long userId) {
		var authentication = getAuthentication();

		if (authentication == null) {
			return false;
		}

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

		return userDetails.getId() == userId;
	}

	public static long getCurrentUserId() {
		var authentication = getAuthentication();

		if (authentication == null ||
			authentication.getPrincipal() instanceof String) {
			return -1;
		}

		return ((UserDetailsImpl) authentication.getPrincipal()).getId();
	}

	public static boolean passwordCheck(String password) {
		if (password.length() < 10) {
			return false;
		}

		return password.matches(".*[A-Z].*") &&
			   password.matches(".*[a-z].*") &&
			   password.matches(".*[0-9].*") &&
			   password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
	}

	public static String passwordHash(String password) {
		var passwordEncoder = new BCryptPasswordEncoder(12);
		return passwordEncoder.encode(password);
	}

	private static Authentication getAuthentication() {
		SecurityContext context = SecurityContextHolder.getContext();
		if (context == null) {
			return null;
		}

		return context.getAuthentication();
	}
}
