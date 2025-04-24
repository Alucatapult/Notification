package com.notification.security;

import com.notification.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditAspect {

    private final AuditLogger auditLogger;

    @AfterReturning(pointcut = "execution(* org.springframework.security.authentication.AuthenticationManager.authenticate(..))", returning = "auth")
    public void afterSuccessfulAuthentication(JoinPoint jp, Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            Map<String, Object> details = new HashMap<>();
            details.put("authorities", auth.getAuthorities());
            
            if (auth.getDetails() instanceof WebAuthenticationDetails) {
                WebAuthenticationDetails webDetails = (WebAuthenticationDetails) auth.getDetails();
                details.put("remoteAddress", webDetails.getRemoteAddress());
                details.put("sessionId", webDetails.getSessionId());
            }
            
            auditLogger.logAuthEvent("LOGIN_SUCCESS", auth.getName(), details);
        }
    }

    @AfterThrowing(pointcut = "execution(* org.springframework.security.authentication.AuthenticationManager.authenticate(..))", throwing = "ex")
    public void afterFailedAuthentication(JoinPoint jp, AuthenticationException ex) {
        String username = "unknown";
        
        if (jp.getArgs() != null && jp.getArgs().length > 0) {
            Object arg = jp.getArgs()[0];
            if (arg instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) arg;
                username = token.getName();
            }
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("exception", ex.getClass().getSimpleName());
        details.put("message", ex.getMessage());
        
        auditLogger.logAuthEvent("LOGIN_FAILURE", username, details);
    }
    
    // Aspect for JWT token validation
    @AfterThrowing(pointcut = "execution(* com.notification.security.JwtTokenUtil.validateToken(..))", throwing = "ex")
    public void afterTokenValidationFailure(JoinPoint jp, Exception ex) {
        String username = "unknown";
        
        if (jp.getArgs() != null && jp.getArgs().length > 1) {
            Object arg = jp.getArgs()[1];
            if (arg instanceof org.springframework.security.core.userdetails.UserDetails) {
                username = ((org.springframework.security.core.userdetails.UserDetails) arg).getUsername();
            }
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("exception", ex.getClass().getSimpleName());
        details.put("message", ex.getMessage());
        
        auditLogger.logAuthEvent("TOKEN_VALIDATION_FAILURE", username, details);
    }
    
    // Aspect for logout (you would need a custom logout success handler)
    @Before("execution(* org.springframework.security.web.authentication.logout.LogoutSuccessHandler.onLogoutSuccess(..))")
    public void beforeLogout(JoinPoint jp) {
        try {
            Object[] args = jp.getArgs();
            if (args != null && args.length > 2 && args[1] instanceof Authentication) {
                Authentication auth = (Authentication) args[1];
                
                Map<String, Object> details = new HashMap<>();
                if (args[0] instanceof jakarta.servlet.http.HttpServletRequest) {
                    jakarta.servlet.http.HttpServletRequest request = (jakarta.servlet.http.HttpServletRequest) args[0];
                    details.put("remoteAddress", request.getRemoteAddr());
                    details.put("sessionId", request.getSession().getId());
                }
                
                auditLogger.logAuthEvent("LOGOUT", auth.getName(), details);
            }
        } catch (Exception e) {
            log.error("Error logging logout event", e);
        }
    }
} 