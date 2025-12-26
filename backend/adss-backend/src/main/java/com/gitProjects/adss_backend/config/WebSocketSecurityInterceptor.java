package com.gitProjects.adss_backend.config;

import com.gitProjects.adss_backend.service.HrAccessValidationService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts WebSocket subscriptions to validate HR manager branch access.
 * Prevents unauthorized subscriptions to branch-specific topics.
 */
@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private final HrAccessValidationService accessValidation;
    
    // Pattern to match /topic/hr/branch/{branchId}
    private static final Pattern BRANCH_TOPIC_PATTERN = Pattern.compile("/topic/hr/branch/(\\d+)");

    public WebSocketSecurityInterceptor(HrAccessValidationService accessValidation) {
        this.accessValidation = accessValidation;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor == null) {
            return message;
        }

        // Only intercept SUBSCRIBE commands
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            
            if (destination != null) {
                Matcher matcher = BRANCH_TOPIC_PATTERN.matcher(destination);
                if (matcher.matches()) {
                    int branchId = Integer.parseInt(matcher.group(1));
                    
                    // Get the user's authentication from the session
                    Authentication auth = (Authentication) accessor.getUser();
                    
                    if (auth == null) {
                        // No authentication - deny subscription
                        throw new IllegalArgumentException("Authentication required to subscribe to branch topics");
                    }
                    
                    // Validate branch access
                    String error = accessValidation.validateBranchAccess(auth, branchId);
                    if (error != null) {
                        throw new IllegalArgumentException("Not authorized to subscribe to branch " + branchId + ": " + error);
                    }
                }
            }
        }

        return message;
    }
}
