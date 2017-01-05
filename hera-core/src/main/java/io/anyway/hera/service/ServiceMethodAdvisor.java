package io.anyway.hera.service;


import io.anyway.hera.collector.MetricsCollector;
import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.common.TraceIdGenerator;
import io.anyway.hera.context.MetricsTraceContext;
import io.anyway.hera.context.MetricsTraceContextHolder;
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
public class ServiceMethodAdvisor implements MethodInterceptor,MetricsCollector,Ordered {

    private long pendingTime= 5*60*1000; //默认5分钟

    final private ConcurrentMap<String,BlockService> blockServiceBuffer = new ConcurrentHashMap<String,BlockService>(2048);

    private MetricsHandler handler;

    private List<Pattern> regExes= Collections.emptyList();

    public void setHandler(MetricsHandler handler){
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
        String atomId= TraceIdGenerator.next();
        //设置该请求的唯一ID
        props.put("atomId",atomId);
        //获取监控上下文
        MetricsTraceContext ctx= MetricsTraceContextHolder.getMetricsTraceContext();
        //如果是本地调用
        if (ctx== null) {
            String traceId= TraceIdGenerator.next();
            MDC.put("traceId",traceId);
            //构造监控上下文
            ctx= new MetricsTraceContext();
            ctx.setTraceId(traceId);
            ctx.setTraceStack(new Stack<String>());
            ctx.setRemote("local");
            MetricsTraceContextHolder.setMetricsTraceContext(ctx);
        }
        //把当前的路径入栈
        ctx.getTraceStack().add(atomId);
        //保存服务调用信息
        blockServiceBuffer.put(atomId,new BlockService(tags,props));
        //执行业务方法
        try{
            return invocation.proceed();
        }catch (Throwable ex){
            //如果存在异常记录异常信息
            Map<String,String> xtags= new LinkedHashMap<String,String>();
            xtags.put("class",ex.getClass().getSimpleName());
            xtags.put("quota", MetricsQuota.SERVICE.toString());
            Map<String,Object> xprops= new LinkedHashMap<String,Object>();
            xprops.put("message",ex.getMessage());
            handler.handle(MetricsQuota.EXCEPTION,xtags,xprops);
            throw ex;
        }
        finally {
            //把当前的路径出栈
            ctx.getTraceStack().pop();
            //删除调用链信息
            blockServiceBuffer.remove(atomId);
            //记录结束时间
            long endTime= System.currentTimeMillis();
            //记录执行的时间
            props.put("duration",endTime - beginTime);
            //发送监控记录
            handler.handle(MetricsQuota.SERVICE,tags,props);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void doCollect() {
        for(Iterator<BlockService> each = blockServiceBuffer.values().iterator(); each.hasNext();){
            BlockService blockService= each.next();
            Map<String,Object> props= blockService.getProps();
            if(System.currentTimeMillis()- (Long)props.get("beginTime")>= pendingTime){
                handler.handle(MetricsQuota.BLOCKSERVICE,blockService.getTags(),props);
                //从阻塞队列中删除
                each.remove();
            }
        }
    }
}

class BlockService{

    Map<String,String> tags;

    Map<String,Object> props;

    BlockService(Map<String,String> tags,Map<String,Object> props){
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
