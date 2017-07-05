package io.anyway.hera.httpclient;

import io.anyway.hera.common.Constants;
import io.anyway.hera.context.MetricTraceContext;
import io.anyway.hera.context.MetricTraceContextHolder;
import io.anyway.hera.service.NonMetricService;
import io.anyway.hera.spring.BeanPostProcessorWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by yangzz on 17/5/19.
 */

@NonMetricService
@Component
public class RestOperationsWrapper implements BeanPostProcessorWrapper {

    private Log logger= LogFactory.getLog(RestOperationsWrapper.class);

    private ClientHttpRequestInterceptor interceptor= new ClientHttpRequestInterceptor(){

        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException{
            final MetricTraceContext ctx= MetricTraceContextHolder.getMetricTraceContext();
            if(ctx== null){
                return execution.execute(request,body);
            }
            return execution.execute(new HttpRequestWrapper(request){

                @Override
                public URI getURI(){
                    URI originalUri = super.getURI();
                    try {
                        URIBuilder uriBuilder = new URIBuilder(originalUri.toString());
                        uriBuilder.addParameter(Constants.TRACE_ID,ctx.getTraceId());
                        uriBuilder.addParameter(Constants.TRACE_PARENT_ID,ctx.getTraceStack().peek());
                        URI newUri= uriBuilder.build();
                        if(logger.isDebugEnabled()){
                            logger.debug("reconstructURI: "+newUri);
                        }
                        return newUri;
                    } catch (URISyntaxException e) {
                        logger.error(e);
                    }
                    return originalUri;
                }
            },body);
        }

    };

    @Override
    public boolean interest(Object bean) {
        return bean instanceof RestTemplate;
    }

    @Override
    public Object wrapBean(Object bean, String appId, String beanName) {
        ((RestTemplate)bean).getInterceptors().add(interceptor);
        logger.info(beanName+" add Interceptor MetricClientHttpRequestInterceptor");
        return bean;
    }

    @Override
    public void destroyWrapper(String appId, String beanName) {

    }
}
