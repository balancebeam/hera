package io.anyway.hera.service;


import io.anyway.hera.collector.MetricCollector;
import io.anyway.hera.collector.MetricHandler;
import io.anyway.hera.common.MetricQuota;
import io.anyway.hera.common.IdGenerator;
import io.anyway.hera.context.MetricTraceContext;
import io.anyway.hera.context.MetricTraceContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by yangzz on 16/8/13.
 */
@NonMetricService
public class ServiceMethodAdvisor implements MethodInterceptor,MetricCollector,Ordered {

    private long pendingTime= 2*60*1000; //默认2分钟

    final private ConcurrentMap<String,LongService> longServices = new ConcurrentHashMap<String,LongService>(2048);

    private MetricHandler handler;

    private List<Pattern> regExes= Collections.emptyList();

    public void setHandler(MetricHandler handler){
        this.handler= handler;
    }

    public void setPendingTime(long pendingTime){
        this.pendingTime= pendingTime;
    }

    public void setPatterns(String patterns) throws ServletException {
        if(!StringUtils.isEmpty(patterns)){
            regExes= new LinkedList<Pattern>();
            for(String each: patterns.split(",")){
                regExes.add(Pattern.compile(each));
            }
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Map<String,String> tags= new LinkedHashMap<String,String>();
        Map<String,Object> props= new LinkedHashMap<String,Object>();
        long beginTime= System.currentTimeMillis();
        //设置调用方法名称
        String methodName= invocation.getThis().getClass().getSimpleName()+"."+invocation.getMethod().getName();
        tags.put("service",methodName);
        if(!regExes.isEmpty()){
            for(Pattern each: regExes){
                Matcher matcher= each.matcher(methodName);
                if(matcher.find()){
                    tags.put("pattern",each.pattern());
                    break;
                }
            }
        }
        //记录请求开始时间
        props.put("beginTime",beginTime);
        //自动生成方法标识
        String spanId= IdGenerator.next();
        //设置该请求的唯一ID
        props.put("spanId",spanId);
        boolean traceable= true;
        //获取监控上下文
        MetricTraceContext ctx= MetricTraceContextHolder.getMetricTraceContext();
        //如果是本地调用
        if (ctx== null) {
            traceable= false;
            String traceId= IdGenerator.next();
            //构造监控上下文
            ctx= new MetricTraceContext();
            ctx.setTraceId(traceId);
            ctx.setTraceStack(new Stack<String>());
            ctx.setRemote("local");
            MetricTraceContextHolder.setMetricTraceContext(ctx);
        }
        //把当前的路径入栈
        ctx.getTraceStack().add(spanId);
        //方便BLOCKSERVICE获取
        props.put("traceId",ctx.getTraceId());
        //保存服务调用信息,并发一万的数据丢弃
        if(longServices.size()< 10000) {
            longServices.put(spanId, new LongService(tags, props));
        }
        //执行业务方法
        try{
            return invocation.proceed();
        }catch (Throwable ex){
            //如果存在异常记录异常信息
            if(!ctx.containException(ex)) {
                ctx.addException(ex);
                Map<String, String> xtags = new LinkedHashMap<String, String>();
                xtags.put("class", ex.getClass().getSimpleName());
                xtags.put("quota", MetricQuota.SERVICE.toString());
                Map<String, Object> xprops = new LinkedHashMap<String, Object>();
                xprops.put("message", ex.getMessage());
                xprops.put("beginTime", System.currentTimeMillis());
                handler.handle(MetricQuota.EXCEPTION, xtags, xprops);
            }
            throw ex;
        }
        finally {
            //把当前的路径出栈
            ctx.getTraceStack().pop();
            //删除调用链信息
            longServices.remove(spanId);
            //记录结束时间
            long endTime= System.currentTimeMillis();
            //记录执行的时间
            props.put("duration",endTime - beginTime);
            //发送监控记录
            handler.handle(MetricQuota.SERVICE,tags,props);
            if(!traceable){
                MetricTraceContextHolder.clear();
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void doCollect() {
        for(Iterator<LongService> each = longServices.values().iterator(); each.hasNext();){
            LongService longService= each.next();
            Map<String,Object> props= longService.getProps();
            if(System.currentTimeMillis()- (Long)props.get("beginTime")>= pendingTime){
                handler.handle(MetricQuota.LONGSERVICE,longService.getTags(),props);
                //从阻塞队列中删除
                each.remove();
            }
        }
    }
}

class LongService {

    Map<String,String> tags;

    Map<String,Object> props;

    LongService(Map<String,String> tags, Map<String,Object> props){
        this.tags= tags;
        this.props= props;
    }

    Map<String,String> getTags(){
        return tags;
    }

    Map<String,Object> getProps(){
        return props;
    }

}
