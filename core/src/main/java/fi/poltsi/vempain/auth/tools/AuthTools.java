package fi.poltsi.vempain.auth.tools;

import fi.poltsi.vempain.auth.exception.VempainAuthenticationException;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class AuthTools {
	private static final String SPECIAL_PASSWORD_CHARACTERS = "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?";

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
			throw new VempainAuthenticationException();
		}

		return ((UserDetailsImpl) authentication.getPrincipal()).getId();
	}

	public static boolean passwordCheck(String password) {
		if (password.length() < 10) {
			return false;
		}

		boolean hasUppercase        = false;
		boolean hasLowercase        = false;
		boolean hasDigit            = false;
		boolean hasSpecialCharacter = false;

		for (int index = 0; index < password.length(); index++) {
			char character = password.charAt(index);
			hasUppercase |= character >= 'A' && character <= 'Z';
			hasLowercase |= character >= 'a' && character <= 'z';
			hasDigit |= character >= '0' && character <= '9';
			hasSpecialCharacter |= SPECIAL_PASSWORD_CHARACTERS.indexOf(character) >= 0;
		}

		return hasUppercase && hasLowercase && hasDigit && hasSpecialCharacter;
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
