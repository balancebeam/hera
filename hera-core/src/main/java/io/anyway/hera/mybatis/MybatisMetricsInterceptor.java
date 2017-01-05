package io.anyway.hera.mybatis;

import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.jdbc.JdbcWrapperHelper;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

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
public class MybatisMetricsInterceptor implements Interceptor {

    private MetricsHandler handler;

    private final Pattern pattern= Pattern.compile("[^\\.]+\\.[^\\.]+$");

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Map<String,Object> props= new LinkedHashMap<String,Object>();
        JdbcWrapperHelper.setMetricsProps(props);
        Map<String,String> tags= new LinkedHashMap<String,String>();
        String mapper= ((MappedStatement)invocation.getArgs()[0]).getId();
        Matcher matcher= pattern.matcher(mapper);
        if(matcher.find()){
            mapper= matcher.group();
        }
        tags.put("mapper",mapper);
        try{
            long beginTime= System.currentTimeMillis();
            Object result= invocation.proceed();
            int size= result instanceof Collection? ((Collection<?>)result).size(): 1;
            props.put("result-size",size);
            long duration= System.currentTimeMillis()- beginTime;
            props.put("result-duration", duration);
            return result;
        }finally{
            JdbcWrapperHelper.setMetricsProps(null);
            handler.handle(MetricsQuota.SQL,tags,props);
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
