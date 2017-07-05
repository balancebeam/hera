package io.anyway.hera.spring;

import io.anyway.hera.service.NonMetricService;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by yangzz on 17/6/15.
 */
@NonMetricService
@Component
public class MetricPointcutAdvisor extends DefaultPointcutAdvisor {

    @Autowired
    @Qualifier("metricServicePointcut")
    private Pointcut pointcut;

    @Autowired
    @Qualifier("metricServiceMethodAdvisor")
    private Advice advice;

    @PostConstruct
    public void init(){
        this.setPointcut(pointcut);
        this.setAdvice(advice);
    }


}
