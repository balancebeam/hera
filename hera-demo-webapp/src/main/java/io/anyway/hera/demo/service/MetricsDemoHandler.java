package io.anyway.hera.demo.service;

import io.anyway.hera.common.MetricsHandler;

import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public class MetricsDemoHandler implements MetricsHandler{

    @Override
    public void send(Map<String, Object> payload) {
        System.out.println(payload);
    }
}
