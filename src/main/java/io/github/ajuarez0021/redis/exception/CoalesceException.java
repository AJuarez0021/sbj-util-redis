package io.github.ajuarez0021.redis.exception;

import java.io.Serial;

public class CoalesceException extends RuntimeException {
    /**
     * The Constant serialVersionUID.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new client exception.
     *
     * @param message the message
     */
    public CoalesceException(String message) {
        super(message);
    }

    /**
     * Instantiates a new client exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public CoalesceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new client exception.
     *
     * @param cause the cause
     */
    public CoalesceException(Throwable cause) {
        super(cause);
    }
}
