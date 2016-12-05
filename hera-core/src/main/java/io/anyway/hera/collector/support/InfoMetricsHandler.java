package io.anyway.hera.collector.support;

import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.context.MetricsTraceContext;
import io.anyway.hera.context.MetricsTraceContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/11/29.
 */
public class InfoMetricsHandler implements MetricsHandler,InitializingBean {

    private Log logger= LogFactory.getLog(InfoMetricsHandler.class);

    private String group;

    private Map<String,String> tags = new LinkedHashMap<String, String>();

    public void setGroup(String group) {
        this.group= group;
    }

    @Override
    public void handle(final MetricsQuota quota, Map<String, String> tags, Map<String, Object> props) {

        Map<String,Object> xprops= new LinkedHashMap<String, Object>(props);
        //获取跟踪链上下文
        MetricsTraceContext ctx= MetricsTraceContextHolder.getMetricsTraceContext();
        if(ctx!= null){
            //设置跟踪链的唯一标识
            xprops.put("traceId",ctx.getTraceId());
            //设置跟踪链栈信息
            xprops.put("traceStack",ctx.getTraceStack().toString());
            //设置用户请求的地址
            xprops.put("remote",ctx.getRemote());
        }
        //构建维度
        Map<String,String> xtags= new LinkedHashMap<String, String>(this.tags);
        if(tags!= null){
            xtags.putAll(tags);
        }

        logger.info("quota: "+quota+" ,tags: "+xtags+" ,props: "+ xprops);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        tags.put("group",group);
        tags.put("host", InetAddress.getLocalHost().getHostAddress());
        tags = Collections.unmodifiableMap(tags);
        if(logger.isInfoEnabled()) {
            logger.info("Metrics Angular: " + tags);
        }
    }
}
