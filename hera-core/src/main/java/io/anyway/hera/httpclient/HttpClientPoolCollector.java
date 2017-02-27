package io.anyway.hera.httpclient;

import io.anyway.hera.collector.MetricsCollector;
import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.common.BlockingStackTraceCollector;
import io.anyway.hera.common.MetricsUtils;
import io.anyway.hera.spring.BeanPostProcessorWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.pool.ConnPoolControl;
import org.apache.http.pool.PoolStats;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by yangzz on 17/1/4.
 */
public class HttpClientPoolCollector implements BeanPostProcessorWrapper,MetricsCollector {

    private Log logger= LogFactory.getLog(HttpClientPoolCollector.class);

    private MetricsHandler handler;

    private BlockingStackTraceCollector blockingStackTraceCollector;

    private final Map<String,ConnPoolControl<HttpRoute>> pool= new LinkedHashMap<String,ConnPoolControl<HttpRoute>>();

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    public void setBlockingStackTraceCollector(BlockingStackTraceCollector blockingStackTraceCollector) {
        this.blockingStackTraceCollector = blockingStackTraceCollector;
    }

    @Override
    public boolean interest(Object bean) {
        return bean instanceof ConnPoolControl;
    }

    @Override
    public Object wrapBean(final Object bean, String appId, final String beanName) {
        Class<?> clazz= bean.getClass();
        Class<?>[] interfaces= MetricsUtils.getInterfaces(clazz,HttpClientStackTraceRepository.class);

        Object result= Proxy.newProxyInstance(clazz.getClassLoader(), interfaces, new InvocationHandler() {
            ConcurrentHashMap<HttpClientConnection,StackTraceElement[]> traceRepository= new ConcurrentHashMap<HttpClientConnection,StackTraceElement[]>();
            @Override
            public Object invoke(Object o, Method method, Object[] args) throws Throwable {
                String methodName= method.getName();
                if("requestConnection".equals(methodName)){
                    final ConnectionRequest delegate= (ConnectionRequest)method.invoke(bean,args);
                    return new ConnectionRequest(){

                        @Override
                        public boolean cancel() {
                            return delegate.cancel();
                        }

                        @Override
                        public HttpClientConnection get(long l, TimeUnit timeUnit)
                                throws InterruptedException, ExecutionException, ConnectionPoolTimeoutException {
                            HttpClientConnection connection= delegate.get(l,timeUnit);
                            traceRepository.put(connection,Thread.currentThread().getStackTrace());
                            return connection;
                        }
                    };
                }
                else if("releaseConnection".equals(methodName)){
                    traceRepository.remove(args[0]);
                }
                else if("getBlockingStackTrace".equals(methodName)){
                    return traceRepository.values();
                }
                return method.invoke(bean,args);
            }
        });
        pool.put((!StringUtils.isEmpty(appId)?appId+":":"")+beanName,(ConnPoolControl<HttpRoute>)result);
        logger.info("metrics HttpClient pool:" +beanName);
        return result;
    }

    @Override
    public void destroyWrapper(String appId, String beanName) {
        pool.remove((!StringUtils.isEmpty(appId)?appId+":":"")+beanName);
        logger.info("remove HttpClient pool:" +beanName);
    }

    @Override
    public void doCollect() {
        for(Map.Entry<String,ConnPoolControl<HttpRoute>> each: pool.entrySet()){
            Map<String,String> tags= new LinkedHashMap<String,String>();
            Map<String,Object> props= new LinkedHashMap<String,Object>();
            tags.put("name",each.getKey());
            ConnPoolControl<HttpRoute> connPoolControl= each.getValue();
            PoolStats stats= connPoolControl.getTotalStats();
            props.put("available",stats.getAvailable());
            props.put("leased",stats.getLeased());
            props.put("max",stats.getMax());
            props.put("pending",stats.getPending());
            handler.handle(MetricsQuota.HTTPCLIENT,tags,props);

            //资源已满打印堆栈
            if(stats.getMax()==stats.getLeased()){
                Collection<StackTraceElement[]> stackTraces= ((HttpClientStackTraceRepository)connPoolControl).getBlockingStackTrace();
                blockingStackTraceCollector.collect(MetricsQuota.HTTPCLIENT,each.getKey(),stackTraces);
            }
        }
    }
}
