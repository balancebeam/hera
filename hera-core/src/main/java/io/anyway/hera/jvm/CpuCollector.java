package io.anyway.hera.jvm;

import io.anyway.hera.collector.MetricHandler;
import io.anyway.hera.common.MetricQuota;
import io.anyway.hera.collector.MetricCollector;
import io.anyway.hera.service.NonMetricService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
@NonMetricService
@Component
public class CpuCollector implements MetricCollector {

    private Log logger= LogFactory.getLog(CpuCollector.class);

    private Method method;

    @Autowired
    private MetricHandler handler;

    public CpuCollector(){
        OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        method = ReflectionUtils.findMethod(operatingSystem.getClass(), "getProcessCpuLoad");
        if(method!= null){
            ReflectionUtils.makeAccessible(method);
            logger.info(operatingSystem.getClass()+".getProcessCpuLoad() collect cpuLoad");
        }
        else{
            logger.info(operatingSystem.getClass()+".getSystemLoadAverage() collect cpuLoad");
        }
    }

    @Override
    public void doCollect() {
        Map<String,Object> props= new LinkedHashMap<String,Object>();
        //获取操作系统
        OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        //获取处理器核数
        props.put("availableProcessors",operatingSystem.getAvailableProcessors());
        //操作系统名称
        props.put("name",operatingSystem.getName());
        //操作系统版本号
        props.put("version",operatingSystem.getVersion());
        if(method!= null){
            double processCpuLoad= (Double) ReflectionUtils.invokeMethod(method,operatingSystem);
            props.put("processCpuLoad", processCpuLoad);
        }
        else{
            double loadedAverage= operatingSystem.getSystemLoadAverage();
            props.put("systemLoadAverage", loadedAverage>0?loadedAverage/operatingSystem.getAvailableProcessors():loadedAverage);
        }
        //发送采集信息
        handler.handle(MetricQuota.CPU,null,props);
    }

//    private boolean isSunOsMBean(OperatingSystemMXBean operatingSystem) {
//        String className = operatingSystem.getClass().getName();
//        return "com.sun.management.OperatingSystem".equals(className)
//                || "com.sun.management.UnixOperatingSystem".equals(className)
//                || "sun.management.OperatingSystemImpl".equals(className);
//    }
}
