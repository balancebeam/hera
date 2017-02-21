package io.anyway.hera.common;

import io.anyway.hera.collector.MetricsHandler;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by yangzz on 17/2/21.
 */
public class BlockingStackTraceCollector {

    private List<Pattern> regExps= Collections.emptyList();

    private MetricsHandler handler;

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    public void setTracePackages(String tracePackages){
        if(StringUtils.isEmpty(tracePackages)){
            throw new IllegalArgumentException("TracePackage not empty.");
        }
        regExps= new LinkedList<Pattern>();
        for(String each: tracePackages.split(",")){
            regExps.add(Pattern.compile(each));
        }
    }

    public void collect(MetricsQuota quota,
                        String pool,
                        Collection<StackTraceElement[]> stackTraces){
        Map<String,Integer> traceMapping= new HashMap<String, Integer>();
        String service= null;
        for(StackTraceElement[] each: stackTraces){
            if(each== null || (service= getMatchingService(each))== null){
                continue;
            }
            if(traceMapping.containsKey(service)){
                traceMapping.put(service,traceMapping.get(service)+1);
            }
            else{
                traceMapping.put(service,1);
            }
        }

        for(Map.Entry<String,Integer> each: traceMapping.entrySet()){
            Map<String,String> tags= new LinkedHashMap<String,String>();
            Map<String,Object> props= new LinkedHashMap<String,Object>();
            tags.put("type",quota.toString());
            tags.put("pool",pool);
            tags.put("service",each.getKey());
            props.put("count",each.getValue());
            handler.handle(MetricsQuota.BLOCKINGSTACKTRACE,tags,props);
        }
    }

    private String getMatchingService(StackTraceElement[] stackTrace){
        for(StackTraceElement each: stackTrace){
            String className= each.getClassName();
            for(Pattern pattern: regExps){
                if(pattern.matcher(className).find() && !className.contains("$")){
                    return className.substring(className.lastIndexOf(".")+1)+"."+each.getMethodName();
                }
            }
        }
        return null;
    }
}
