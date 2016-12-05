package io.anyway.hera.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yangzz on 16/8/17.
 */
public class MetricsBeanPostProcessor implements BeanPostProcessor, PriorityOrdered,DisposableBean{

    private Map<String,BeanPostProcessorWrapper> wrapperIdx= new HashMap<String, BeanPostProcessorWrapper>();

    private int order = LOWEST_PRECEDENCE;

    private String appId;

    private List<BeanPostProcessorWrapper> beanPostProcessorWrappers = Collections.emptyList();

    public void setAppId(String appId){
        this.appId= appId;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        for(BeanPostProcessorWrapper each: beanPostProcessorWrappers){
            if(each.interest(bean)){
                wrapperIdx.put(beanName,each);
                return each.wrapBean(bean,appId,beanName);
            }
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setBeanPostProcessorWrappers(List<BeanPostProcessorWrapper> beanPostProcessorWrappers){
        this.beanPostProcessorWrappers = beanPostProcessorWrappers;
    }

    @Override
    public void destroy() throws Exception {
        for (Map.Entry<String,BeanPostProcessorWrapper> each: wrapperIdx.entrySet()){
            each.getValue().destroyWrapper(appId,each.getKey());
        }
    }
}
