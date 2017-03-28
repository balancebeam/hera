package io.anyway.hera.web;

import io.anyway.hera.collector.MetricHandler;
import io.anyway.hera.common.Constants;
import io.anyway.hera.common.MetricQuota;
import io.anyway.hera.common.MetricUtils;
import io.anyway.hera.common.IdGenerator;
import io.anyway.hera.context.MetricTraceContext;
import io.anyway.hera.context.MetricTraceContextHolder;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yangzz on 16/8/16.
 */
public class MetricFilter implements Filter {

    private ServletContext servletContext;

    private List<Pattern> regExes= Collections.emptyList();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext= filterConfig.getServletContext();
        String patterns= filterConfig.getInitParameter("patterns");
        if(!StringUtils.isEmpty(patterns)){
            regExes= new LinkedList<Pattern>();
            for(String each: patterns.split(",")){
                regExes.add(Pattern.compile(each));
            }
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        ApplicationContext applicationContext= MetricUtils.getWebApplicationContext(servletContext);
        MetricHandler handler= applicationContext.getBean(MetricHandler.class);
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
        Map<String,String> tags= null;
        //设置该请求的唯一ID
        props.put("spanId",spanId);
        //设置请求的http URL
        String url= request.getRequestURI();
        props.put("url",url);
        //添加匹配路径
        if(!regExes.isEmpty()){
            for(Pattern each: regExes){
                Matcher matcher= each.matcher(url);
                if(matcher.find()){
                    tags= new LinkedHashMap<String, String>();
                    tags.put("pattern",each.pattern());
                    break;
                }
            }
        }
        //记录请求开始时间
        props.put("beginTime", beginTime);
        //把当前的路径入栈
        traceStack.add(spanId);

        try{
            //调用过滤链
            chain.doFilter(req,res);
        }catch (Throwable ex){
            //如果存在异常记录异常信息
            if(!ctx.containException(ex)) {
                ctx.addException(ex);
                Map<String, String> xtags = new LinkedHashMap<String, String>();
                xtags.put("class", ex.getClass().getSimpleName());
                xtags.put("quota", MetricQuota.HTTP.toString());
                Map<String, Object> xprops = new LinkedHashMap<String, Object>();
                xprops.put("message", ex.getMessage());
                xprops.put("beginTime", System.currentTimeMillis());
                handler.handle(MetricQuota.EXCEPTION, xtags, xprops);
            }

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
            MetricTraceContextHolder.getMetricTraceContext().getTraceStack().pop();
            //记录结束时间
            long endTime= System.currentTimeMillis();
            //记录执行的时间
            props.put("duration",endTime-beginTime);
            //发送监控记录
            handler.handle(MetricQuota.HTTP,tags,props);
            //清空上下文
            MetricTraceContextHolder.clear();
        }
    }

    @Override
    public void destroy() {
        //do noting
    }
}
