package io.anyway.hera.spring;

/**
 * Created by yangzz on 16/8/19.
 */
public interface BeanPostProcessorWrapper {

    boolean interest(Object bean);

    Object wrapBean(Object bean,String appId, String beanName);

    void destroyWrapper(String appId,String beanName);
}
