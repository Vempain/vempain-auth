package fi.poltsi.vempain.auth.api;

public class Constants {
	public static final Long ADMIN_ID = 1L;
	public static final String REST_CONTENT_PREFIX = "/content-management";
	public static final String REST_FILE_PREFIX = REST_CONTENT_PREFIX + "/file";
	public static final String REST_SCHEDULE_PREFIX = "/schedule-management";
	public static final String REST_ADMIN_PREFIX = "/admin-management";
	public static final String LOGIN_PATH = "/login";
	public static final String TEST_PATH_PREFIX = "/test";

	private Constants() {
		throw new IllegalStateException("Constants class");
	}
}
