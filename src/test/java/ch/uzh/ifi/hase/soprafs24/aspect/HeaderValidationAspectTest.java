package ch.uzh.ifi.hase.soprafs24.aspect;

import ch.uzh.ifi.hase.soprafs24.annotation.AuthorizationRequired;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class HeaderValidationAspectTest {

    @Mock
    private UserService userService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletRequestAttributes attributes;

    @InjectMocks
    private HeaderValidationAspect headerValidationAspect;

    private Method method;

    // Sample test method with AuthorizationRequired annotation
    @AuthorizationRequired(headerName = "Authorization")
    public void sampleMethod() {
        // This is just a placeholder method for testing
    }
    
    @BeforeEach
    void setUp() throws NoSuchMethodException {
        MockitoAnnotations.openMocks(this);
        
        // Get the actual method to use for testing
        method = HeaderValidationAspectTest.class.getMethod("sampleMethod");
        
        // Set up mocks
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        
        // Create a real ServletRequestAttributes with our mocked request
        attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
    }
    
    @Test
    void validateHeader_WithoutValidToken_ShouldThrowException() throws Throwable {
        // Arrange
        String token = "invalid-token";
        String bearerToken = "Bearer " + token;
        
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")).when(userService).validateToken(token);
        
        // Assert
        assertThrows(ResponseStatusException.class, () -> {
            headerValidationAspect.validateHeader(joinPoint);
        });
        
        // Verify that we called validateToken but never proceeded
        verify(userService).validateToken(token);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void validateHeader_WithValidToken_ShouldProceed() throws Throwable {
        // Arrange
        String token = "valid-token";
        String bearerToken = "Bearer " + token;
        
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(joinPoint.proceed()).thenReturn("Result");
        when(userService.validateToken(token)).thenReturn(new User());
        
        // Act
        Object result = headerValidationAspect.validateHeader(joinPoint);
        
        // Assert
        assertEquals("Result", result);
        verify(userService).validateToken(token);
        verify(joinPoint).proceed();
    }

    @Test
    void validateHeader_WithTokenWithoutBearerPrefix_ShouldProceed() throws Throwable {
        // Arrange
        String token = "valid-token";
        
        when(request.getHeader("Authorization")).thenReturn(token);
        when(joinPoint.proceed()).thenReturn("Result");
        when(userService.validateToken(token)).thenReturn(new User());
        
        // Act
        Object result = headerValidationAspect.validateHeader(joinPoint);
        
        // Assert
        assertEquals("Result", result);
        verify(userService).validateToken(token);
        verify(joinPoint).proceed();
    }

    @Test
    void validateHeader_WithNoRequestAttributes_ShouldProceed() throws Throwable {
        // Arrange
        RequestContextHolder.resetRequestAttributes();
        when(joinPoint.proceed()).thenReturn("Result");
        
        // Act
        Object result = headerValidationAspect.validateHeader(joinPoint);
        
        // Assert
        assertEquals("Result", result);
        verifyNoInteractions(userService);
        verify(joinPoint).proceed();
    }
}
