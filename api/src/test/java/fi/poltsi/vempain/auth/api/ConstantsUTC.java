package fi.poltsi.vempain.auth.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The published path constants every backend and SPA build their routes from, and the fact that the holder can not be instantiated.
 */
class ConstantsUTC {

    @Test
    void constructorIsPrivateAndRefusesInstantiation() throws NoSuchMethodException {
        var constructor = Constants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);

        var exception = assertThrows(InvocationTargetException.class, constructor::newInstance);

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("Constants class", exception.getCause()
            .getMessage());
    }

    @Test
    void pathConstantsAreStable() {
        assertEquals(1L, Constants.ADMIN_ID);
        assertEquals("/content-management", Constants.REST_CONTENT_PREFIX);
        assertEquals("/file-management", Constants.REST_FILE_PREFIX);
        assertEquals("/schedule-management", Constants.REST_SCHEDULE_PREFIX);
        assertEquals("/admin-management", Constants.REST_ADMIN_PREFIX);
        assertEquals("/login", Constants.LOGIN_PATH);
        assertEquals("/test", Constants.TEST_PATH_PREFIX);
    }
}
