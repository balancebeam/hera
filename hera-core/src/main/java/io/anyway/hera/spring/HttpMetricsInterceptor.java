package io.anyway.hera.spring;

import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.Constants;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.common.TraceIdGenerator;
import io.anyway.hera.context.MetricsTraceContext;
import io.anyway.hera.context.MetricsTraceContextHolder;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Created by yangzz on 16/11/20.
 */
public class HttpMetricsInterceptor implements HandlerInterceptor,Ordered {

    private ThreadLocal<Map<String,Object>> holder= new ThreadLocal<Map<String,Object>>();

    private MetricsHandler handler;

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        holder.remove();

        //如果在Filter处理过,或者嵌套拦截器处理过就不处理
        if(MetricsTraceContextHolder.getMetricsTraceContext()!= null){
            return true;
        }

        //获取传入跟踪链的信息
        while(request instanceof HttpServletRequestWrapper){
            request= (HttpServletRequest)((HttpServletRequestWrapper)request).getRequest();
        }
        //获取跟踪链的信息
        String traceId= request.getHeader(Constants.TRACE_ID);
        String traceStrackInput= request.getHeader(Constants.TRACE_STACK);
        if(StringUtils.isEmpty(traceId)){
            traceId= request.getParameter(Constants.TRACE_ID);
            traceStrackInput= request.getParameter(Constants.TRACE_STACK);
        }
        Stack<String> traceStack= new Stack<String>();
        //如果调用链为空则需要创建一个调用链
        if(StringUtils.isEmpty(traceId)){
            traceId= TraceIdGenerator.next();
            if(!StringUtils.isEmpty(traceStrackInput)) {
                traceStack.addAll(Arrays.asList(traceStrackInput.split(",")));
            }
        }
        //把跟踪链的信息绑定到日志里,方便做日志跟踪
        MDC.put("traceId",traceId);
        //构造监控上下文
        MetricsTraceContext ctx= new MetricsTraceContext();
        ctx.setTraceId(traceId);
        ctx.setTraceStack(traceStack);
        ctx.setRemote(request.getRemoteHost());
        //绑定监控上下文到Threadlocal
        MetricsTraceContextHolder.setMetricsTraceContext(ctx);

        String atomId= TraceIdGenerator.next();
        long beginTime= System.currentTimeMillis();

        Map<String,Object> props= new LinkedHashMap<String,Object>();
        //设置该请求的唯一ID
        props.put("atomId",atomId);
        //设置请求的http URL
        props.put("url",request.getRequestURI());
        //记录请求开始时间
        props.put("beginTime", beginTime);
        //把当前的路径入栈
        traceStack.add(atomId);
        holder.set(props);

        return true;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) throws Exception {
        //获取拦截器preHandle中收集的内容
        Map<String,Object> props= holder.get();
        if(props== null){
            return;
        }
        //处理异常
        if(ex!= null){
            Map<String,String> xtags= new LinkedHashMap<String,String>();
            xtags.put("class",ex.getClass().getSimpleName());
            xtags.put("type", MetricsQuota.HTTP.toString());
            Map<String,Object> xprops= new LinkedHashMap<String,Object>();
            xprops.put("message",ex.getMessage());
            xprops.put("timestamp",System.currentTimeMillis());
            this.handler.handle(MetricsQuota.EXCEPTION,xtags,xprops);
        }

        MetricsTraceContextHolder.getMetricsTraceContext().getTraceStack().pop();
        //记录结束时间
        long endTime= System.currentTimeMillis();
        //记录时间
        props.put("timestamp",endTime);
        //记录执行的时间
        props.put("duration",endTime-(Long)props.get("beginTime"));
        //发送监控记录
        this.handler.handle(MetricsQuota.HTTP,null,props);
        //清空上下文变量
        MetricsTraceContextHolder.clear();
        holder.remove();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
