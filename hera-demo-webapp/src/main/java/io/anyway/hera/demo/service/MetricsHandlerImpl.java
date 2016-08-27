package io.anyway.hera.demo.service;

import io.anyway.hera.common.MetricsHandler;
import io.anyway.hera.common.MetricsType;
import io.anyway.hera.context.MetricsTraceContext;
import io.anyway.hera.context.MetricsTraceContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
@Component
public class MetricsHandlerImpl implements MetricsHandler {


    @Override
    public void handle(String type,Map<String,String> angular,Map<String,Object> payload) {

        payload.putAll(angular);
        payload.put("type",type);

        System.out.println(payload);
    }
}
