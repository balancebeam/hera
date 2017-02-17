package io.anyway.hera.jdbc;

import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.collector.MetricsCollector;
import io.anyway.hera.spring.BeanPostProcessorWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by yangzz on 16/8/17.
 */
public class DataSourceCollector implements BeanPostProcessorWrapper,MetricsCollector {

    private Log logger= LogFactory.getLog(DataSourceCollector.class);

    private Map<String,JdbcWrapper> jdbcWrappers= new HashMap<String, JdbcWrapper>();

    private List<String> excludedDatasources;

    private MetricsHandler handler;

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    /**
     * Connection阻塞的跟踪有效包路径
     * @param holdInterestTracePackages
     */
    public void setHoldInterestTracePackages(String holdInterestTracePackages){
        if(holdInterestTracePackages==null ||
                "".equals(holdInterestTracePackages=holdInterestTracePackages.trim())) {
            throw new IllegalArgumentException("HOLD_INTEREST_TRACE_PACKAGES should be not empty.");
        }
        List<String> holdPackages= Arrays.asList(holdInterestTracePackages.split(","));
        HoldConnectionInformations.HOLD_INTEREST_TRACE_PACKAGES = holdPackages;
        logger.info("HOLD_INTEREST_TRACE_PACKAGES: "+holdPackages);
    }

    /**
     * 设置监控数据源的属性内容,默认 dbcp | druid 不用配置,c3p0 | jndi数据源需要配置
     * @param configMetadata
     */
    public void setConfigMetadata(String configMetadata){
        if(!StringUtils.isEmpty(configMetadata)){
            Map<String,String> hash= new LinkedHashMap<String, String>();
            for(String each: configMetadata.split(",")){
                String[] kv= each.split(":");
                hash.put(kv[0],kv[1]);
            }
            JdbcWrapper.DATASOURCE_CONFIG_METADATA= hash;
            logger.info("CONFIG_METADATA: "+hash);
        }
    }

    /**
     * 不需要监控的数据源
     * @param excludedDatasources
     */
    public void setExcludedDatasources(String excludedDatasources) {
        if(!StringUtils.isEmpty(excludedDatasources)) {
            this.excludedDatasources = Arrays.asList(excludedDatasources.split(","));
        }
    }


    private boolean isExcludedDataSource(String beanName) {
        if (excludedDatasources != null && excludedDatasources.contains(beanName)) {
            logger.info("Spring datasource excluded: " + beanName);
            return true;
        }
        //如果是内部类不需要被代理
        return beanName.matches("\\(inner bean\\).+");
    }

    private Object createProxy(final Object bean, final String appId,final String beanName) {
        final InvocationHandler invocationHandler = new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object result = method.invoke(bean, args);
                if (result instanceof DataSource) {
                    JdbcWrapper jdbcWrapper= new JdbcWrapper(handler);
                    jdbcWrappers.put((StringUtils.isEmpty(appId)?"":appId+":")+beanName,jdbcWrapper);
                    result = jdbcWrapper.createDataSourceProxy(beanName,(DataSource) result);
                }
                return result;
            }
        };
        return JdbcWrapper.createProxy(bean, invocationHandler);
    }

    @Override
    public boolean interest(Object bean) {
        return (bean instanceof DataSource) || (bean instanceof JndiObjectFactoryBean);
    }

    @Override
    public synchronized Object wrapBean(Object bean, String appId, String beanName) {
        try {
            if (bean instanceof DataSource) {
                if (isExcludedDataSource(beanName)) {
                    return bean;
                }
                if (!jdbcWrappers.containsKey(beanName)) {
                    final DataSource dataSource = (DataSource) bean;
                    JdbcWrapper jdbcWrapper = new JdbcWrapper(handler);
                    jdbcWrappers.put((StringUtils.isEmpty(appId)?"":appId+":")+beanName, jdbcWrapper);
                    final DataSource result = jdbcWrapper.createDataSourceProxy(beanName, dataSource);
                    logger.info("Spring datasource wrapped: " + beanName);
                    return result;
                }
            } else if (bean instanceof JndiObjectFactoryBean) {
                if (isExcludedDataSource(beanName)) {
                    return bean;
                }
                final Object result = createProxy(bean, appId,beanName);
                logger.info("Spring JNDI factory wrapped: " + beanName);
                return result;
            }
        }catch (Throwable e){
            logger.error("Spring wrap datasource error",e);
        }
        return bean;
    }

    @Override
    public void doCollect() {
        for (Map.Entry<String,JdbcWrapper> each: jdbcWrappers.entrySet()){
            JdbcWrapper jdbcWrapper= each.getValue();
            Map<String,String> tags= new LinkedHashMap<String,String>();
            Map<String,Object> props= new LinkedHashMap<String, Object>();
            tags.put("dataSourceName",each.getKey());
            props.put("maxActive",jdbcWrapper.getMaxActive());
            props.put("maxWait",jdbcWrapper.getMaxWait());
            props.put("url",jdbcWrapper.getUrl());
            props.put("initialSize",jdbcWrapper.getInitialSize());
            props.put("maxIdle",jdbcWrapper.getMaxIdle());
            props.put("minIdle",jdbcWrapper.getMinIdle());
            props.put("activeCount",jdbcWrapper.getActiveConnectionCount());
            props.put("usedCount",jdbcWrapper.getUsedConnectionCount());
            props.put("holdCount",jdbcWrapper.getHoldedConnectionCount());

            handler.handle(MetricsQuota.JDBC,tags,props);

            //连接阻塞发出阻塞的服务列表和调用次数
            if(jdbcWrapper.getHoldedConnectionCount()==jdbcWrapper.getMaxActive()){
                Map<String,Integer> holdServices= new HashMap<String, Integer>();
                //汇总阻塞方法
                for(HoldConnectionInformations info: jdbcWrapper.getHoldedConnectionInformationsList()){
                    String service= info.getMatchingHoldService();
                    if (service== null){
                        continue;
                    }
                    if(holdServices.containsKey(service)){
                        holdServices.put(service,holdServices.get(service)+1);
                    }
                    else{
                        holdServices.put(service,1);
                    }
                }
                for(Map.Entry<String,Integer> entry: holdServices.entrySet()){
                    Map<String,String> btags= new LinkedHashMap<String,String>();
                    Map<String,Object> bprops= new LinkedHashMap<String, Object>();
                    btags.put("dataSourceName",each.getKey());
                    btags.put("service",entry.getKey());
                    bprops.put("count",entry.getValue());
                    handler.handle(MetricsQuota.HOLDCONNECTION,btags,bprops);
                }
            }
        }
    }

    @Override
    public synchronized void destroyWrapper(String appId,String beanName){
        jdbcWrappers.remove((StringUtils.isEmpty(appId)?"":appId+":")+beanName);
    }
}
