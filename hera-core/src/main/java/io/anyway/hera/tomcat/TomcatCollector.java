package io.anyway.hera.tomcat;

import io.anyway.hera.collector.MetricsCollector;
import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.jvm.MBeans;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.*;

/**
 * Created by yangzz on 16/12/20.
 */
public class TomcatCollector implements MetricsCollector {

    private final Log logger= LogFactory.getLog(TomcatCollector.class);

    private final boolean TOMCAT_USED = System.getProperty("catalina.home") != null;

    private MBeans mBeans;

    private List<ObjectName> THREAD_POOLS;

    private Map<String,ObjectName> GLOBAL_REQUEST_PROCESSORS;

    private MetricsHandler handler;

    public TomcatCollector(){
        if (!TOMCAT_USED) {
            return;
        }
        logger.info("Catalina Tomcat Server");
        mBeans = new MBeans();
        THREAD_POOLS = new ArrayList<ObjectName>();
        GLOBAL_REQUEST_PROCESSORS = new LinkedHashMap<String,ObjectName>();

        try {
            THREAD_POOLS.addAll(mBeans.getTomcatThreadPools());
            logger.info("Tomcat Thread Pools: "+THREAD_POOLS);
            Set<ObjectName> globalRequestProcessors= mBeans.getTomcatGlobalRequestProcessors();
            for(ObjectName each: THREAD_POOLS){
                String name= each.getKeyProperty("name");
                for (ObjectName grp : globalRequestProcessors) {
                    if (name.equals(grp.getKeyProperty("name"))) {
                        GLOBAL_REQUEST_PROCESSORS.put(name,grp);
                        break;
                    }
                }
            }

        } catch (MalformedObjectNameException e) {
            logger.error(e);
        }
    }

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    @Override
    public void doCollect() {
        if (!TOMCAT_USED) {
            return;
        }
        try {
            for(ObjectName each: THREAD_POOLS){
                Map<String,String> tags= new LinkedHashMap<String,String>();
                Map<String,Object> props= new LinkedHashMap<String,Object>();
                String name= each.getKeyProperty("name");
                tags.put("name",name);
                props.put("maxThreads",(Integer)mBeans.getAttribute(each, "maxThreads"));
                props.put("currentThreadCount",(Integer)mBeans.getAttribute(each, "currentThreadCount"));
                props.put("currentThreadsBusy",(Integer)mBeans.getAttribute(each, "currentThreadsBusy"));
                if(GLOBAL_REQUEST_PROCESSORS.containsKey(name)){
                    ObjectName grp= GLOBAL_REQUEST_PROCESSORS.get(name);
                    props.put("bytesReceived",(Long) mBeans.getAttribute(grp, "bytesReceived"));
                    props.put("bytesSent",(Long) mBeans.getAttribute(grp, "bytesSent"));
                    props.put("requestCount",(Integer) mBeans.getAttribute(grp, "requestCount"));
                    props.put("errorCount",(Integer) mBeans.getAttribute(grp, "errorCount"));
                    props.put("processingTime",(Long) mBeans.getAttribute(grp, "processingTime"));
                    props.put("maxTime",(Long) mBeans.getAttribute(grp, "maxTime"));
                }
                handler.handle(MetricsQuota.TOMCAT,tags,props);
            }
        } catch (JMException e) {
            logger.error(e);
        }
    }
}
