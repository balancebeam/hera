package io.anyway.hera.httpclient;

import io.anyway.hera.common.Constants;
import io.anyway.hera.context.MetricTraceContext;
import io.anyway.hera.context.MetricTraceContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;

/**
 * Created by yangzz on 17/6/15.
 */
public class MetricCloseableHttpClient extends CloseableHttpClient {

    private static Log logger= LogFactory.getLog(MetricCloseableHttpClient.class);

    private CloseableHttpClient proxy;

    final private Method method;

    public MetricCloseableHttpClient(CloseableHttpClient proxy){
        this.proxy= proxy;
        method= ReflectionUtils.findMethod(CloseableHttpClient.class,"doExecute",new Class<?>[]{HttpHost.class,HttpRequest.class,HttpContext.class});
        ReflectionUtils.makeAccessible(method);
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
        MetricTraceContext ctx= MetricTraceContextHolder.getMetricTraceContext();
        if(ctx!= null){
            HttpRequestBase httpRequestBase= (HttpRequestBase)httpRequest;
            URIBuilder uriBuilder;
            try{
                uriBuilder= new URIBuilder(httpRequestBase.getURI().toString());
                uriBuilder.addParameter(Constants.TRACE_ID,ctx.getTraceId());
                uriBuilder.addParameter(Constants.TRACE_PARENT_ID,ctx.getTraceStack().peek());
                httpRequestBase.setURI(uriBuilder.build());
                httpRequest= httpRequestBase;
            }catch (URISyntaxException e){
                logger.error(e);
            }
        }
        return (CloseableHttpResponse)ReflectionUtils.invokeMethod(method,proxy,new Object[]{httpHost,httpRequest,httpContext});
    }

    @Override
    public void close() throws IOException {
        proxy.close();
    }

    @Override
    public HttpParams getParams() {
        return proxy.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return proxy.getConnectionManager();
    }
}
