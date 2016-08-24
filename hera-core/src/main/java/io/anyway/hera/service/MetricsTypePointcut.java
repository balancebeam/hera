package io.anyway.hera.service;

import io.anyway.hera.annotation.Metrics;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Created by yangzz on 16/8/13.
 */
public class MetricsTypePointcut implements Pointcut {

    private Class<? extends Annotation>[] pointcutTypes= new Class[]{Metrics.class};

    final private MetricsMethodMatcher metricsMethodMatcher;

    public MetricsTypePointcut(){
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

    public void setPointcutTypes(Class<? extends Annotation>[] pointcutTypes){
        this.pointcutTypes= pointcutTypes;
    }

    private class MetricsMethodMatcher implements MethodMatcher {

        private MetricsMethodMatcher(){
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public boolean matches(Method method, Class targetClass) {
            for(Class<? extends Annotation> each: pointcutTypes){
                try {
                    if (targetClass.isAnnotationPresent(each)
                            || method.getDeclaringClass().isAnnotationPresent(each)
                            || method.isAnnotationPresent(each)
                            || targetClass.getDeclaredMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(each)) {
                        return true;
                    }
                }catch (Exception e){}
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
