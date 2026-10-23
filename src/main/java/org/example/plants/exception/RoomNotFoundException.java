package org.example.plants.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A room named in a request body does not exist. That is a bad request rather than a
 * 404: the URL names a resource that is there, the body names one that is not.
 * The {@code @ResponseStatus} below lets Spring map it, so no controller has to.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(Long roomId) {
        super("Room " + roomId + " does not exist.");
    }
}
