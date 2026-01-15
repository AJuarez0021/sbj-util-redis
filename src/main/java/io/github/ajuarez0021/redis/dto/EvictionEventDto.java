package io.github.ajuarez0021.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * The Class EvictionEventDto.
 *
 * @author ajuar
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvictionEventDto implements java.io.Serializable {

    /** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The key. */
    private String key;
    
    /** The timestamp. */
    private LocalDateTime timestamp;
}
