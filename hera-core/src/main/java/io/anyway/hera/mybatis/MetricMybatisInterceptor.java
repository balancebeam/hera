package io.anyway.hera.mybatis;

import io.anyway.hera.collector.MetricHandler;
import io.anyway.hera.common.IdGenerator;
import io.anyway.hera.common.MetricQuota;
import io.anyway.hera.context.MetricTraceContext;
import io.anyway.hera.context.MetricTraceContextHolder;
import io.anyway.hera.service.NonMetricService;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yangzz on 17/1/3.
 */
@Intercepts({
        @Signature(type = Executor.class,
                method = "update",
                args = {MappedStatement.class,
                        Object.class}),
        @Signature(type = Executor.class,
                method = "query",
                args = {MappedStatement.class,
                        Object.class,
                        RowBounds.class,
                        ResultHandler.class}),
        @Signature(type = Executor.class,
                method = "query",
                args = {MappedStatement.class,
                        Object.class,
                        RowBounds.class,
                        ResultHandler.class,
                        CacheKey.class,
                        BoundSql.class})
})
@NonMetricService
@Component("metricMybatisInterceptor")
public class MetricMybatisInterceptor implements Interceptor {

    @Autowired
    private MetricHandler handler;

    private final Pattern pattern= Pattern.compile("[^\\.]+\\.[^\\.]+$");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Map<String,Object> props= new LinkedHashMap<String,Object>();
        Map<String,String> tags= new LinkedHashMap<String,String>();
        String mapper= ((MappedStatement)invocation.getArgs()[0]).getId();
        Matcher matcher= pattern.matcher(mapper);
        if(matcher.find()){
            mapper= matcher.group();
        }
        tags.put("mapper",mapper);
        MetricTraceContext ctx= MetricTraceContextHolder.getMetricTraceContext();
        if(ctx!= null){
            String spanId= IdGenerator.next();
            props.put("spanId",spanId);
            ctx.getTraceStack().add(spanId);
        }
        try{
            long beginTime= System.currentTimeMillis();
            props.put("beginTime",beginTime);
            Object result= invocation.proceed();
            int size= result instanceof Collection? ((Collection<?>)result).size(): 1;
            props.put("size",size);
            long duration= System.currentTimeMillis()- beginTime;
            props.put("duration", duration);
            return result;
        }finally{
            if(ctx!= null){
                ctx.getTraceStack().pop();
            }
            handler.handle(MetricQuota.MAPPER,tags,props);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target,this);
    }

    @Override
    public void setProperties(Properties properties) {
        //do nothing
    }
}
