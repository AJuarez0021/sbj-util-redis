package io.github.ajuarez0021.redis.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * The Class CoalescedResponseDto.
 *
 * @author ajuar
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoalescedResponseDto implements Serializable{
    
    /** The Constant serialVersionUID. */
	private static final long serialVersionUID = 8579398868082295820L;
	
	/** The request id. */
	private String requestId;
    
    /** The coalescing key. */
    private String coalescingKey;
    
    /** The result. */
    private Object result;
    
    /** The error. */
    private Throwable error;
    
    /** The completed at. */
    private LocalDateTime completedAt;
}
