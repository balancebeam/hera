package io.anyway.hera.common;

import io.anyway.hera.context.MetricsTraceContext;
import io.anyway.hera.context.MetricsTraceContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.Assert;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/8/23.
 */
public class MetricsManager implements InitializingBean,ApplicationListener<ContextRefreshedEvent> {

    private static Log logger= LogFactory.getLog(MetricsManager.class);

    private static String group;

    private static Map<String,String> angular = new LinkedHashMap<String, String>();

    private static MetricsHandler handler;

    private static ThreadLocal<SimpleDateFormat> simpleDateFormatHolder= new ThreadLocal<SimpleDateFormat>();

    public void setGroup(String group) {
        MetricsManager.group= group;
    }

    public void setHandler(MetricsHandler handler){
       this.handler= handler;
    }

    final public static void collect(MetricsType type, Map<String,Object> payload){

        Map<String,Object> fieldPayload= new LinkedHashMap<String, Object>(payload);
        //获取跟踪链上下文
        MetricsTraceContext ctx= MetricsTraceContextHolder.getMetricsTraceContext();
        if(ctx!= null){
            //设置跟踪链的唯一标识
            fieldPayload.put("transactionId",ctx.getTraceId());
            //设置跟踪链栈信息
            fieldPayload.put("transactionTrace",ctx.getTraceStack().toString());
            //设置用户请求的地址
            fieldPayload.put("remote",ctx.getRemote());
        }
        if(handler!=null) {
            try {
                handler.handle(type.toString(), angular, fieldPayload);
            }catch (Throwable e){
                logger.error("Handle "+type.toString()+" metrics information error",e);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(group);
        String host= InetAddress.getLocalHost().getHostAddress();
        angular.put("group",group);
        angular.put("host",host);
        angular = Collections.unmodifiableMap(angular);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(handler== null) {
            handler = event.getApplicationContext().getBean(MetricsHandler.class);
        }
    }

    public static String toLocalDate(long time){
        SimpleDateFormat sdf= simpleDateFormatHolder.get();
        if(sdf ==null){
            sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ms");
            simpleDateFormatHolder.set(sdf);
        }
        return sdf.format(new Date(time));
    }
}
