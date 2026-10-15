package fi.poltsi.vempain.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Getter;

@Getter
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "The object was not found")
public class VempainEntityNotFoundException extends Exception {

    private final String message;
    private final String entityName;

    public VempainEntityNotFoundException(String message, String entityName) {
        super();
        this.message = message;
        this.entityName = entityName;
    }

    public VempainEntityNotFoundException() {
        super();
        this.message = "The object was not found";
        this.entityName = "";
    }
}
