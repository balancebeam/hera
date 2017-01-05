package io.anyway.hera.spring;

/**
 * Created by yangzz on 17/1/4.
 */
public interface BeanPreProcessorWrapper {

    boolean interest(Object bean);

    Object preWrapBean(Object bean,String ctxId,String beanName);
}
