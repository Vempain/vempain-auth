package fi.poltsi.vempain.auth.tools;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;

@Slf4j
public class TestUserAccountTools {
	private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+[]{}|;:,.<>?";

	public static String encryptPassword(String password) {
		var pwdEncryptor = new BCryptPasswordEncoder(12);
		return pwdEncryptor.encode(password);
	}

	public static String randomLongString() {
		return RandomStringUtils.secure()
								.nextAlphanumeric(14);
	}

	public static String randomPassword(int length) {
		SecureRandom  secureRandom  = new SecureRandom();
		StringBuilder stringBuilder = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			int randomIndex = secureRandom.nextInt(CHAR_POOL.length());
			stringBuilder.append(CHAR_POOL.charAt(randomIndex));
		}

		return stringBuilder.toString();
	}
}
