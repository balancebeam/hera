package io.anyway.hera.mybatis;

import io.anyway.hera.service.NonMetricService;
import io.anyway.hera.spring.BeanPreProcessorWrapper;
import org.apache.ibatis.plugin.Interceptor;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * Created by yangzz on 17/1/3.
 */
@NonMetricService
public class MybatisPluginProcessor implements BeanPreProcessorWrapper{

    @Autowired
    @Qualifier("metricMybatisInterceptor")
    private Interceptor interceptor;

    @Override
    public boolean interest(Object bean) {
        return bean instanceof SqlSessionFactoryBean;
    }

    @Override
    public Object preWrapBean(Object bean, String ctxId, String beanName) {
        SqlSessionFactoryBean obj= (SqlSessionFactoryBean)bean;
        Field field = ReflectionUtils.findField(SqlSessionFactoryBean.class,"plugins");
        if(field!= null){
            ReflectionUtils.makeAccessible(field);
            Interceptor[] plugins= (Interceptor[])ReflectionUtils.getField(field,obj);
            if(plugins== null){
                ReflectionUtils.setField(field, obj, new Interceptor[]{interceptor});
            }
            else{
                Interceptor[] nPlugins= new Interceptor[plugins.length+1];
                nPlugins[0]= interceptor;
                System.arraycopy(plugins,0,nPlugins,1,plugins.length);
                ReflectionUtils.setField(field, obj,nPlugins);
            }
        }
        return bean;
    }
}
