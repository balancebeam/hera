package io.anyway.hera.jvm;

import io.anyway.hera.common.MetricsType;
import io.anyway.hera.common.MetricsManager;
import io.anyway.hera.common.MetricsCollector;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public class CpuCollector implements MetricsCollector {

    @Override
    public void doCollect() {
        Map<String,Object> payload= new LinkedHashMap<String,Object>();
        //设置内存类别
        //payload.put("category","cpu");
        //获取操作系统
        OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        //获取处理器核数
        payload.put("availableProcessors",operatingSystem.getAvailableProcessors());
        //操作系统名称
        payload.put("name",operatingSystem.getName());
        //操作系统版本号
        payload.put("version",operatingSystem.getVersion());
        //获取处理器核数
        payload.put("systemLoadAverage",operatingSystem.getSystemLoadAverage());
        //采集时间
        payload.put("timestamp",MetricsManager.toLocalDate(System.currentTimeMillis()));
        //发送采集信息
        MetricsManager.collect(MetricsType.CPU,payload);
    }
}
