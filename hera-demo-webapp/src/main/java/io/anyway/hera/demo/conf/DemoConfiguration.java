package io.anyway.hera.demo.conf;

import io.anyway.hera.configuration.MetricConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by yangzz on 17/6/15.
 */
@Configuration
@Import(MetricConfiguration.class)
public class DemoConfiguration {
}
