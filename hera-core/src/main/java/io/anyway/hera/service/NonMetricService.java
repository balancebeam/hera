package io.anyway.hera.service;

import java.lang.annotation.*;

/**
 * Created by yangzz on 16/8/16.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface NonMetricService {
    String value() default "";
}
