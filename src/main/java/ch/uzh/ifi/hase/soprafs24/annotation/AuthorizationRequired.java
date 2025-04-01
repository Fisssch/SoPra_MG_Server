package ch.uzh.ifi.hase.soprafs24.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a REST endpoint requires a specific header.
 * Use this on controller methods that need header validation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthorizationRequired {
    /**
     * The name of the required header.
     */
    String headerName() default "Authorization";
}
