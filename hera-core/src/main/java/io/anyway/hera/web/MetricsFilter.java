package io.anyway.hera.web;

import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.Constants;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.common.MetricsUtils;
import io.anyway.hera.common.TraceIdGenerator;
import io.anyway.hera.context.MetricsTraceContext;
import io.anyway.hera.context.MetricsTraceContextHolder;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;

/**
 * Created by yangzz on 16/8/16.
 */
public class MetricsFilter implements Filter {

    private ServletContext servletContext;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext= filterConfig.getServletContext();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        ApplicationContext applicationContext= MetricsUtils.getWebApplicationContext(servletContext);
        MetricsHandler handler= applicationContext.getBean(MetricsHandler.class);
        if(handler== null){
            chain.doFilter(req,res);
            return;
        }

        HttpServletRequest request= (HttpServletRequest)req;
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

        try{
            //调用过滤链
            chain.doFilter(req,res);
        }catch (Throwable ex){
            //如果存在异常记录异常信息
            Map<String,String> xtags= new LinkedHashMap<String,String>();
            xtags.put("class",ex.getClass().getSimpleName());
            xtags.put("quota", MetricsQuota.HTTP.toString());
            Map<String,Object> xprops= new LinkedHashMap<String,Object>();
            xprops.put("message",ex.getMessage());
            xprops.put("timestamp",System.currentTimeMillis());
            handler.handle(MetricsQuota.EXCEPTION,xtags,xprops);

            if(ex instanceof IOException){
                throw (IOException)ex;
            }
            if(ex instanceof ServletException){
                throw (ServletException)ex;
            }
            if(ex instanceof RuntimeException){
                throw (RuntimeException)ex;
            }
        }
        finally{
            MetricsTraceContextHolder.getMetricsTraceContext().getTraceStack().pop();
            //记录结束时间
            long endTime= System.currentTimeMillis();
            props.put("timestamp",endTime);
            //记录执行的时间
            props.put("duration",endTime-beginTime);
            //发送监控记录
            handler.handle(MetricsQuota.HTTP,null,props);
            //清空上下文
            MetricsTraceContextHolder.clear();
        }
    }

    @Override
    public void destroy() {
        //do noting
    }
}
