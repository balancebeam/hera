package io.anyway.hera.common;

import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public interface MetricsResourceHandler {

    MetricsType getType();

    void handle(Map<String,String> systemDimension,Map<String,Object> payload);
}
