package io.anyway.hera.httpclient;

import io.anyway.hera.common.Constants;
import io.anyway.hera.common.MetricsUtils;
import io.anyway.hera.context.MetricsTraceContext;
import io.anyway.hera.context.MetricsTraceContextHolder;
import io.anyway.hera.spring.BeanPostProcessorWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by yangzz on 17/2/27.
 */
public class HttpClientBeanProcessor implements BeanPostProcessorWrapper {

    private Log logger= LogFactory.getLog(HttpClientBeanProcessor.class);

    @Override
    public boolean interest(Object bean) {
        return bean instanceof HttpClient;
    }

    @Override
    public Object wrapBean(final Object bean, String appId, String beanName) {
        Class<?> clazz= bean.getClass();
        Class<?>[] interfaces= MetricsUtils.getInterfaces(clazz);
        Object result= Proxy.newProxyInstance(clazz.getClassLoader(),interfaces,new InvocationHandler(){
            ThreadLocal<Boolean> sign= new ThreadLocal<Boolean>();
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                MetricsTraceContext ctx= MetricsTraceContextHolder.getMetricsTraceContext();
                if(ctx!= null && sign.get()== null && "execute".equals(method.getName())){
                    for(Object each: args){
                        if(each instanceof HttpRequestBase){
                            HttpRequestBase httpRequestBase= (HttpRequestBase)each;
                            URIBuilder uriBuilder= new URIBuilder(httpRequestBase.getURI().toString());
                            uriBuilder.addParameter(Constants.TRACE_ID,ctx.getTraceId());
                            uriBuilder.addParameter(Constants.TRACE_STACK,ctx.getTraceStack().peek());
                            httpRequestBase.setURI(uriBuilder.build());
                            if(logger.isDebugEnabled()){
                                logger.debug("add trace to http query parameters  : "+httpRequestBase.getURI().toString());
                            }
                            sign.set(true);
                            try{
                                return method.invoke(bean,args);
                            }finally {
                                sign.remove();
                            }
                        }
                    }
                }
                return method.invoke(bean,args);
            }
        });
        logger.info("metrics httpclient :"+ beanName);
        return result;
    }

    @Override
    public void destroyWrapper(String appId, String beanName) {
        //do nothing
    }
}
