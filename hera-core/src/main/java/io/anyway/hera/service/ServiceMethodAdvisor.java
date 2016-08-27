package io.anyway.hera.service;


import io.anyway.hera.common.MetricsType;
import io.anyway.hera.common.MetricsManager;
import io.anyway.hera.common.TransactionIdGenerator;
import io.anyway.hera.context.MetricsTraceContext;
import io.anyway.hera.context.MetricsTraceContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.Ordered;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Created by yangzz on 16/8/13.
 */
public class ServiceMethodAdvisor implements MethodInterceptor,Ordered {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        Map<String,Object> payload= new LinkedHashMap<String,Object>();

        long beginTime= System.currentTimeMillis();
        //设置类别为http
        //payload.put("category","service");
        //设置行为规则为进入
        payload.put("action","in");
        //设置调用方法名称
        payload.put("service",invocation.getThis().getClass().getName()+"."+invocation.getMethod().getName());
        //记录请求开始时间
        payload.put("timestamp",MetricsManager.toLocalDate(beginTime));
        //发送监控记录
        MetricsManager.collect(MetricsType.SERVICE,payload);
        //获取监控上下文
        MetricsTraceContext ctx= MetricsTraceContextHolder.getMetricsTraceContext();
        //把当前的路径入栈
        if (ctx!= null) {
            //自动生成方法标识
            String atomId= TransactionIdGenerator.next();
            //设置该请求的唯一ID
            payload.put("atomId",atomId);
            ctx.getTransactionTrace().add(atomId);
        }
        try{
            return invocation.proceed();
        }catch (Throwable e){
            //如果存在异常记录异常信息
            payload.put("exception",e.getMessage());
            throw e;
        }
        finally {
            //把当前的路径出栈
            if (ctx!= null) {
                ctx.getTransactionTrace().pop();
            }
            //记录结束时间
            long endTime= System.currentTimeMillis();
            //记录请求结束时间
            payload.put("timestamp",MetricsManager.toLocalDate(endTime));
            //记录执行的时间
            payload.put("duration",endTime-beginTime);
            //更改行为规则为出去
            payload.put("action","out");
            //发送监控记录
            MetricsManager.collect(MetricsType.SERVICE,payload);
        }

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
