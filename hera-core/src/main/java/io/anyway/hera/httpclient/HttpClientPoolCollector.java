package io.anyway.hera.httpclient;

import io.anyway.hera.collector.MetricsCollector;
import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.spring.BeanPostProcessorWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.pool.ConnPoolControl;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 17/1/4.
 */
public class HttpClientPoolCollector implements BeanPostProcessorWrapper,MetricsCollector {

    private Log logger= LogFactory.getLog(HttpClientPoolCollector.class);

    private MetricsHandler handler;

    private final Map<String,ConnPoolControl<HttpRoute>> pool= new LinkedHashMap<String,ConnPoolControl<HttpRoute>>();

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    @Override
    public boolean interest(Object bean) {
        return bean instanceof ConnPoolControl;
    }

    @Override
    public Object wrapBean(Object bean, String appId, String beanName) {
        pool.put((!StringUtils.isEmpty(appId)?appId+":":"")+beanName,(ConnPoolControl<HttpRoute>)bean);
        logger.info("metrics HttpClient pool:" +beanName);
        return bean;
    }

    @Override
    public void destroyWrapper(String appId, String beanName) {
        pool.remove((!StringUtils.isEmpty(appId)?appId+":":"")+beanName);
        logger.info("remove HttpClient pool:" +beanName);
    }

    @Override
    public void doCollect() {
        for(Map.Entry<String,ConnPoolControl<HttpRoute>> each: pool.entrySet()){
            Map<String,String> tags= new LinkedHashMap<String,String>();
            Map<String,Object> props= new LinkedHashMap<String,Object>();
            tags.put("name",each.getKey());
            ConnPoolControl<HttpRoute> connPoolControl= each.getValue();
            props.put("available",connPoolControl.getTotalStats().getAvailable());
            props.put("leased",connPoolControl.getTotalStats().getLeased());
            props.put("max",connPoolControl.getTotalStats().getMax());
            props.put("pending",connPoolControl.getTotalStats().getPending());
            handler.handle(MetricsQuota.HTTPCLIENT,tags,props);
        }
    }
}
