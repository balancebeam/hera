package io.anyway.hera.spring;

import io.anyway.hera.collector.MetricHandler;
import io.anyway.hera.common.Constants;
import io.anyway.hera.common.MetricQuota;
import io.anyway.hera.common.IdGenerator;
import io.anyway.hera.context.MetricTraceContext;
import io.anyway.hera.context.MetricTraceContextHolder;
import io.anyway.hera.service.NonMetricService;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yangzz on 16/11/20.
 */
@NonMetricService
public class MetricHttpInterceptor implements HandlerInterceptor,Ordered {

    private ThreadLocal<Map<String,Object>> holder= new ThreadLocal<Map<String,Object>>();

    private MetricHandler handler;

    private List<Pattern> regExes= Collections.emptyList();

    public void setHandler(MetricHandler handler){
        this.handler= handler;
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
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        holder.remove();

        //如果在Filter处理过,或者嵌套拦截器处理过就不处理
        if(MetricTraceContextHolder.getMetricTraceContext()!= null){
            return true;
        }

        //获取传入跟踪链的信息
        while(request instanceof HttpServletRequestWrapper){
            request= (HttpServletRequest)((HttpServletRequestWrapper)request).getRequest();
        }
        //获取跟踪链的信息
        String traceId= request.getHeader(Constants.TRACE_ID);
        String parentId= request.getHeader(Constants.TRACE_PARENT_ID);
        if(StringUtils.isEmpty(traceId)){
            traceId= request.getParameter(Constants.TRACE_ID);
            parentId= request.getParameter(Constants.TRACE_PARENT_ID);
        }
        Stack<String> traceStack= new Stack<String>();
        //如果调用链为空则需要创建一个调用链
        if(StringUtils.isEmpty(traceId)){
            traceId= IdGenerator.next();
        }
        if(!StringUtils.isEmpty(parentId)) {
            traceStack.push(parentId);
        }
        //把跟踪链的信息绑定到日志里,方便做日志跟踪
        MDC.put("traceId",traceId);
        //构造监控上下文
        MetricTraceContext ctx= new MetricTraceContext();
        ctx.setTraceId(traceId);
        ctx.setTraceStack(traceStack);
        ctx.setRemote(request.getRemoteHost());
        //绑定监控上下文到Threadlocal
        MetricTraceContextHolder.setMetricTraceContext(ctx);

        String spanId= IdGenerator.next();
        long beginTime= System.currentTimeMillis();

        Map<String,Object> props= new LinkedHashMap<String,Object>();

        //设置该请求的唯一ID
        props.put("spanId",spanId);
        //设置请求的http URL
        props.put("url",request.getRequestURI());
        //记录请求开始时间
        props.put("beginTime", beginTime);
        //把当前的路径入栈
        traceStack.add(spanId);
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
            xtags.put("quota", MetricQuota.HTTP.toString());
            Map<String,Object> xprops= new LinkedHashMap<String,Object>();
            xprops.put("message",ex.getMessage());
            xprops.put("beginTime",System.currentTimeMillis());
            this.handler.handle(MetricQuota.EXCEPTION,xtags,xprops);
        }

        MetricTraceContextHolder.getMetricTraceContext().getTraceStack().pop();
        //记录结束时间
        long endTime= System.currentTimeMillis();
        Map<String,String> tags= null;
        //记录执行的时间
        props.put("duration",endTime-(Long)props.get("beginTime"));

        if(!regExes.isEmpty()){
            String url= (String)props.get("url");
            for(Pattern each: regExes){
                Matcher matcher= each.matcher(url);
                if(matcher.find()){
                    tags= new LinkedHashMap<String, String>();
                    tags.put("pattern",each.pattern());
                    break;
                }
            }
        }
        //发送监控记录
        this.handler.handle(MetricQuota.HTTP,tags,props);
        //清空上下文变量
        MetricTraceContextHolder.clear();
        holder.remove();
        MDC.remove("traceId");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
