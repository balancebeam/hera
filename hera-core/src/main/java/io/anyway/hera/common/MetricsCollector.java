package io.anyway.hera.common;

import io.anyway.hera.context.MetricsContext;
import io.anyway.hera.context.MetricsContextHolder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/8/23.
 */
public class MetricsCollector implements InitializingBean{

    private final static Map<String,String> sysPayload= new LinkedHashMap<String, String>();

    private static MetricsHandler handler;

    public void setGroup(String group) {
        sysPayload.put("group",group);
    }

    public void setHandler(MetricsHandler handler){
        MetricsCollector.handler= handler;
    }

    final public static void collect(Map<String,Object> payload){
        Map<String,Object> result= new LinkedHashMap<String, Object>();
        result.putAll(sysPayload);
        MetricsContext ctx= MetricsContextHolder.getMetricsContext();
        if(ctx!= null){
            //设置跟踪链的唯一标识
            result.put("transactionId",ctx.getTransactionId());
            //设置跟踪链栈信息
            result.put("transactionTrace",ctx.getTransactionTrace());
            //设置用户请求的地址
            result.put("remote",ctx.getRemote());
        }
        result.putAll(payload);
        handler.send(result);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(StringUtils.isEmpty((sysPayload.get("group")))){
            throw new IllegalArgumentException("group empty");
        }
        Assert.notNull(handler);
        sysPayload.put("host", InetAddress.getLocalHost().getHostAddress());
    }
}
