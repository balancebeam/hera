package io.anyway.hera.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.Assert;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/8/23.
 */
public class MetricsUnifiedCollector implements InitializingBean,ApplicationListener<ContextRefreshedEvent> {

    private static Log logger= LogFactory.getLog(MetricsUnifiedCollector.class);

    private static String group;

    private static Map<String,String> systemDimension= new LinkedHashMap<String, String>();

    private static Map<MetricsType,MetricsResourceHandler> handlers= Collections.EMPTY_MAP;

    public void setGroup(String group) {
        MetricsUnifiedCollector.group= group;
    }

    final public static void collect(MetricsType type, Map<String,Object> payload){

        MetricsResourceHandler handler= handlers.get(type);
        if(handler== null){
            //logger.warn("Miss "+type+ " metrics handler");
            return;
        }
        handler.handle(systemDimension,new LinkedHashMap<String, Object>(payload));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(group);
        String host= InetAddress.getLocalHost().getHostAddress();
        systemDimension.put("group",group);
        systemDimension.put("host",host);
        systemDimension= Collections.unmodifiableMap(systemDimension);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Map<String,MetricsResourceHandler> hash= event.getApplicationContext().getBeansOfType(MetricsResourceHandler.class);
        if(hash!= null) {
            handlers= new LinkedHashMap<MetricsType, MetricsResourceHandler>();
            for (MetricsResourceHandler each : hash.values()) {
                handlers.put(each.getType(),each);
            }
            logger.info("MetricsResourceHandlers:"+handlers);
        }
    }
}
