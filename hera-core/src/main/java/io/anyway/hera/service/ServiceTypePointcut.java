package io.anyway.hera.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.Advised;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yangzz on 16/8/13.
 */
@NonMetricService
@Component("metricServicePointcut")
public class ServiceTypePointcut implements Pointcut {

    private Log logger= LogFactory.getLog(ServiceTypePointcut.class);

    private Class<? extends Annotation>[] pointcutTypes= new Class[]{MetricService.class};

    final private MetricsMethodMatcher metricsMethodMatcher;

    public ServiceTypePointcut(){
        metricsMethodMatcher= new MetricsMethodMatcher();
    }

    @Override
    public ClassFilter getClassFilter() {
        return ClassFilter.TRUE;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return metricsMethodMatcher;
    }

    public void setServicePointcutTypes(String servicePointcutTypes){
        if(!StringUtils.isEmpty(servicePointcutTypes)){
            List<Class<? extends Annotation>> result= new LinkedList<Class<? extends Annotation>>();
            for(String each: servicePointcutTypes.split(",")){
                try {
                    ClassLoader loader= Thread.currentThread().getContextClassLoader();
                    result.add((Class<? extends Annotation>)loader.loadClass(each));
                } catch (ClassNotFoundException e) {
                    logger.error(e);
                }
            }
            if(!CollectionUtils.isEmpty(result)) {
                if(!result.contains(MetricService.class)){
                    result.add(MetricService.class);
                }
                this.pointcutTypes = result.toArray(new Class[result.size()]);
            }
        }
    }

    private class MetricsMethodMatcher implements MethodMatcher {

        private MetricsMethodMatcher(){
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public boolean matches(Method method, Class targetClass) {
            for(Class<? extends Annotation> each: pointcutTypes){
                try {
                    if (!Advised.class.isAssignableFrom(targetClass)
                            && (targetClass.isAnnotationPresent(each)
                            || method.getDeclaringClass().isAnnotationPresent(each)
                            || method.isAnnotationPresent(each)
                            || targetClass.getMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(each))) {
                        return true;
                    }
                }catch (Exception e){
                    //logger.error(e,e);
                }
            }
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isRuntime() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("rawtypes")
        public boolean matches(Method method, Class targetClass, Object[] args) {
            throw new UnsupportedOperationException("This is not a runtime method matcher");
        }
    }
}
