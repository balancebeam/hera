package io.anyway.hera.collector;

/**
 * Created by yangzz on 16/8/23.
 */
public interface MetricCollector {
    /**
     * 采集各指标数据
     */
    void doCollect();
}
