package io.anyway.hera.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;


/**
 * Created by yangzz on 17/6/15.
 */
//@EnableWebMvc
@EnableAspectJAutoProxy
@Configuration
@ComponentScan(basePackages = "io.anyway.hera")
public class MetricConfiguration{
}
