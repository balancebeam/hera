package io.anyway.hera.collector;

import io.anyway.hera.common.MetricsQuota;

import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public interface MetricsHandler {

    void handle(MetricsQuota quota, Map<String,String> tags, Map<String,Object> props);
}
