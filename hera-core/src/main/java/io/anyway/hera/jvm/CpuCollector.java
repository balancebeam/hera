package io.anyway.hera.jvm;

import io.anyway.hera.common.MetricsType;
import io.anyway.hera.common.MetricsManager;
import io.anyway.hera.common.MetricsCollector;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public class CpuCollector implements MetricsCollector {

    @Override
    public void doCollect() {
        Map<String,Object> payload= new LinkedHashMap<String,Object>();
        //获取操作系统
        OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        //获取处理器核数
        payload.put("availableProcessors",operatingSystem.getAvailableProcessors());
        //操作系统名称
        payload.put("name",operatingSystem.getName());
        //操作系统版本号
        payload.put("version",operatingSystem.getVersion());
        //cpu负载值
        double loadedAverage= operatingSystem.getSystemLoadAverage();
        payload.put("systemLoadAverage",loadedAverage);
        //windows获取的cpu负载值为-1
        if(loadedAverage== -1.0 && isSunOsMBean(operatingSystem)){
            try {
                loadedAverage= Double.parseDouble(BeanUtils.getProperty(operatingSystem,"systemCpuLoad"));
                payload.put("systemLoadAverage", loadedAverage);
            } catch (Exception e) {}
        }
        //采集时间
        payload.put("timestamp",MetricsManager.toLocalDate(System.currentTimeMillis()));
        //发送采集信息
        MetricsManager.collect(MetricsType.CPU,payload);
    }

    private boolean isSunOsMBean(OperatingSystemMXBean operatingSystem) {
        String className = operatingSystem.getClass().getName();
        return "com.sun.management.OperatingSystem".equals(className)
                || "com.sun.management.UnixOperatingSystem".equals(className)
                || "sun.management.OperatingSystemImpl".equals(className);
    }
}
