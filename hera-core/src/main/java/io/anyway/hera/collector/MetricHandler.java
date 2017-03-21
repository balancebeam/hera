package io.anyway.hera.collector;

import io.anyway.hera.common.MetricQuota;

import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public interface MetricHandler {

    void handle(MetricQuota quota, Map<String,String> tags, Map<String,Object> props);
}
