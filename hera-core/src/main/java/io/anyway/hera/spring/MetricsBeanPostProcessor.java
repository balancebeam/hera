package io.anyway.hera.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;

import java.util.Collections;
import java.util.List;

/**
 * Created by yangzz on 16/8/17.
 */
public class MetricsBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    private int order = LOWEST_PRECEDENCE;

    private List<BeanPostProcessorWrapper> beanPostProcessorWrappers = Collections.emptyList();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        for(BeanPostProcessorWrapper each: beanPostProcessorWrappers){
            if(each.interest(bean)){
                return each.wrapBean(bean,beanName);
            }
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setBeanPostProcessorWrappers(List<BeanPostProcessorWrapper> beanPostProcessorWrappers){
        this.beanPostProcessorWrappers = beanPostProcessorWrappers;
    }

}
