package fi.poltsi.vempain.auth.api;

public class Constants {

    public static final Long ADMIN_ID = 1L;
    public static final String REST_CONTENT_PREFIX = "/content-management";
    public static final String REST_FILE_PREFIX = "/file-management";
    public static final String REST_SCHEDULE_PREFIX = "/schedule-management";
    public static final String REST_ADMIN_PREFIX = "/admin-management";
    public static final String LOGIN_PATH = "/login";
    /**
     * Response header carrying the renewed session token: every authenticated request that is not a background request answers with a fresh token whose
     * lifetime starts at that request, so the session lives {@code vempain.app.jwt-expiration-ms} from the user's last action. Clients replace their stored
     * token with it.
     */
    public static final String RENEWED_TOKEN_HEADER = "X-Vempain-Token";
    /** Response header next to {@link #RENEWED_TOKEN_HEADER}: the expiry of the renewed token as an ISO-8601 instant. */
    public static final String RENEWED_TOKEN_EXPIRES_AT_HEADER = "X-Vempain-Token-Expires-At";
    /**
     * Request header a client sets to {@code true} on requests the user did not trigger (task polling, status refreshes): they are served as usual but do not
     * renew the session, so an open tray or shell does not keep an idle session alive.
     */
    public static final String BACKGROUND_REQUEST_HEADER = "X-Vempain-Background";
    public static final String TEST_PATH_PREFIX = "/test";

    private Constants() {
        throw new IllegalStateException("Constants class");
    }
}
