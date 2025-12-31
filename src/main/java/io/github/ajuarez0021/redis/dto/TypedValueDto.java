package io.github.ajuarez0021.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The Class TypedValue.
 */
@Data
@AllArgsConstructor
public class TypedValueDto {
    /** The type. */
    private String type;

    /** The value. */
    private Object value;
}
