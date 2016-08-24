package io.anyway.hera.demo.service;

import io.anyway.hera.common.MetricsResourceHandler;
import io.anyway.hera.common.MetricsType;
import io.anyway.hera.context.MetricsContext;
import io.anyway.hera.context.MetricsContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
@Component
public class MetricsJDBCHandler implements MetricsResourceHandler {

    @Override
    public MetricsType getType() {
        return MetricsType.JDBC;
    }

    @Override
    public void handle(final Map<String,String> systemDimension, final Map<String,Object> payload) {
        payload.putAll(systemDimension);
        payload.put("category","jdbc");
        System.out.println(payload);
    }
}
