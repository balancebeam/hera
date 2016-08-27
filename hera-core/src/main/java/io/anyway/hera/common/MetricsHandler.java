package io.anyway.hera.common;

import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public interface MetricsHandler {

    void handle(String type,Map<String,String> angular,Map<String,Object> payload);
}
