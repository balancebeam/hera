package io.anyway.hera.web;

import io.anyway.hera.common.Constants;
import io.anyway.hera.common.MetricsType;
import io.anyway.hera.common.MetricsManager;
import io.anyway.hera.common.TraceIdGenerator;
import io.anyway.hera.context.MetricsTraceContext;
import io.anyway.hera.context.MetricsTraceContextHolder;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

/**
 * Created by yangzz on 16/8/16.
 */
public class MetricsFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
       //do noting
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        //获取传入跟踪链的信息
        HttpServletRequest request= (HttpServletRequest)req;
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

        Map<String,Object> payload= new LinkedHashMap<String,Object>();
        //设置该请求的唯一ID
        payload.put("atomId",atomId);
        //设置行为规则为进入
        payload.put("action","in");
        //设置请求的http URL
        payload.put("url",request.getContextPath()+request.getServletPath());
        //设置异常默认为false
        payload.put("exception",false);
        //记录请求开始时间
        payload.put("timestamp",MetricsManager.toLocalDate(beginTime));
        //发送监控记录
        MetricsManager.collect(MetricsType.HTTP,payload);

        //把当前的路径入栈
        traceStack.add(atomId);
        try{
            //调用过滤链
            chain.doFilter(req,res);
        }catch (Throwable e){
            //如果存在异常记录异常信息
            payload.put("exception",e.getMessage());

            if(e instanceof IOException){
                throw (IOException)e;
            }
            if(e instanceof ServletException){
                throw (ServletException)e;
            }
            if(e instanceof RuntimeException){
                throw (RuntimeException)e;
            }
        }
        finally{
            //把当前的路径出栈
            traceStack.pop();
            //记录结束时间
            long endTime= System.currentTimeMillis();
            //记录请求结束时间
            payload.put("timestamp",MetricsManager.toLocalDate(endTime));
            //记录执行的时间
            payload.put("duration",endTime-beginTime);
            //更改行为规则为出去
            payload.put("action","out");
            //发送监控记录
            MetricsManager.collect(MetricsType.HTTP,payload);
            //清空上下文变量
            MetricsTraceContextHolder.clear();
        }
    }

    @Override
    public void destroy() {
        //do noting
    }
}
