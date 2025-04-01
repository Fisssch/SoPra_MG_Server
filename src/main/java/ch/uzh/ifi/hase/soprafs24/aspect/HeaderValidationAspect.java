package ch.uzh.ifi.hase.soprafs24.aspect;

import ch.uzh.ifi.hase.soprafs24.annotation.AuthorizationRequired;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
public class HeaderValidationAspect {
        
    private final UserService userService;
    
    public HeaderValidationAspect(UserService userService) {
        this.userService = userService;
    }

    @Around("@annotation(ch.uzh.ifi.hase.soprafs24.annotation.AuthorizationRequired)")
    public Object validateHeader(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get HTTP request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = attributes.getRequest();
        
        // Get the method that was called
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // Get our custom annotation
        AuthorizationRequired requiredHeaderAnnotation = method.getAnnotation(AuthorizationRequired.class);
        String requiredHeaderName = requiredHeaderAnnotation.headerName();
        
        String headerValue = request.getHeader(requiredHeaderName);
        
        if (headerValue.startsWith("Bearer ")) {
            headerValue = headerValue.substring(7); // Remove "Bearer " prefix
        }

        // Check if the header is present and valid
        userService.validateToken(headerValue);
        
        // Continue with the method execution
        return joinPoint.proceed();
    }
}