package io.anyway.hera.httpclient;

import io.anyway.hera.service.NonMetricService;
import io.anyway.hera.spring.BeanPostProcessorWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.stereotype.Component;

/**
 * Created by yangzz on 17/2/27.
 */
@NonMetricService
@Component
public class HttpClientBeanProcessor implements BeanPostProcessorWrapper {

    private Log logger= LogFactory.getLog(HttpClientBeanProcessor.class);

    @Override
    public boolean interest(Object bean) {
        return bean instanceof HttpClient;
    }

    @Override
    public Object wrapBean(final Object bean, String appId, String beanName) {
        logger.info("metric httpclient :"+ beanName);
        return new MetricCloseableHttpClient((CloseableHttpClient)bean);
    }

    @Override
    public void destroyWrapper(String appId, String beanName) {
        //do nothing
    }
}
