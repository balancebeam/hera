package io.anyway.hera.jdbc;

import io.anyway.hera.common.MetricsCollector;
import io.anyway.hera.scheduler.MetricsProcessor;
import io.anyway.hera.spring.BeanPostProcessorWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by yangzz on 16/8/17.
 */
public class DataSourceBeanPostProcessor implements BeanPostProcessorWrapper,ServletContextAware,MetricsProcessor {

    private Log logger= LogFactory.getLog(DataSourceBeanPostProcessor.class);

    private Map<String,JdbcWrapper> jdbcWrappers= new HashMap<String, JdbcWrapper>();

    private Set<String> excludedDatasources;

    private ServletContext servletContext;

    /**
     * Connection泄露的跟踪有效包路径
     * @param traceInterestPackages
     */
    public void setTraceInterestPackages(List<String> traceInterestPackages){
        if(!StringUtils.isEmpty(traceInterestPackages)){
            ConnectionInformations.TRACE_INTEREST_PACKAGES = traceInterestPackages;
            logger.info("TRACE_INTEREST_PACKAGES: "+traceInterestPackages);
        }
    }

    /**
     * 设置监控数据源的属性内容,默认 dbcp | druid 不用配置,c3p0 | jndi数据源需要配置
     * @param dataSourceConfigMetadata
     */
    public void setDataSourceConfigMetadata(Map<String,String> dataSourceConfigMetadata){
        if(dataSourceConfigMetadata!=null && !dataSourceConfigMetadata.isEmpty()){
            JdbcWrapper.DATASOURCE_CONFIG_METADATA= dataSourceConfigMetadata;
            logger.info("CONFIG_METADATA: "+dataSourceConfigMetadata);
        }
    }

    /**
     * 不需要监控的数据源
     * @param excludedDatasources
     */
    public void setExcludedDatasources(Set<String> excludedDatasources) {
        this.excludedDatasources = excludedDatasources;
    }

    private boolean isExcludedDataSource(String beanName) {
        if (excludedDatasources != null && excludedDatasources.contains(beanName)) {
            logger.info("Spring datasource excluded: " + beanName);
            return true;
        }
        return false;
    }

    private Object createProxy(final Object bean, final String beanName) {
        final InvocationHandler invocationHandler = new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object result = method.invoke(bean, args);
                if (result instanceof DataSource) {
                    JdbcWrapper jdbcWrapper= new JdbcWrapper(servletContext);
                    jdbcWrappers.put(beanName,jdbcWrapper);
                    result = jdbcWrapper.createDataSourceProxy(beanName,(DataSource) result);
                }
                return result;
            }
        };
        return JdbcWrapper.createProxy(bean, invocationHandler);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext= servletContext;
    }

    @Override
    public boolean interest(Object bean) {
        return (bean instanceof DataSource) || (bean instanceof JndiObjectFactoryBean);
    }

    @Override
    public Object wrapBean(Object bean, String beanName) {
        if (bean instanceof DataSource) {
            if (isExcludedDataSource(beanName)) {
                return bean;
            }
            final DataSource dataSource = (DataSource) bean;
            JdbcWrapper jdbcWrapper= new JdbcWrapper(servletContext);
            jdbcWrappers.put(beanName,jdbcWrapper);
            final DataSource result = jdbcWrapper.createDataSourceProxy(beanName,dataSource);
            logger.info("Spring datasource wrapped: " + beanName);
            return result;
        } else if (bean instanceof JndiObjectFactoryBean) {
            if (isExcludedDataSource(beanName)) {
                return bean;
            }
            final Object result = createProxy(bean, beanName);
            logger.info("Spring JNDI factory wrapped: " + beanName);
            return result;
        }
        return bean;
    }

    @Override
    public void doMonitor() {
        for (Map.Entry<String,JdbcWrapper> each: jdbcWrappers.entrySet()){
            JdbcWrapper jdbcWrapper= each.getValue();
            Map<String,Object> payload= new LinkedHashMap<String, Object>();
            payload.put("category","jdbc");
            payload.put("name",each.getKey());
            payload.put("maxActive",jdbcWrapper.getMaxActive());
            payload.put("maxWait",jdbcWrapper.getMaxWait());
            payload.put("activeCount",jdbcWrapper.getActiveConnectionCount());
            payload.put("usedCount",jdbcWrapper.getUsedConnectionCount());
            payload.put("holdedCount",jdbcWrapper.getHoldedConnectionCount());
            List<Map<String,Object>> traceList= new LinkedList<Map<String, Object>>();
            for (ConnectionInformations info: jdbcWrapper.getHoldedConnectionInformationsList()){
                Map<String,Object> hash= new LinkedHashMap<String, Object>();
                hash.put("openingTime",info.getOpeningTime());
                hash.put("stackTrace",info.getOpeningStackTrace().toString());
                traceList.add(hash);
            }
            payload.put("holdedTraceList",traceList);
            payload.put("timestamp",System.currentTimeMillis());
            MetricsCollector.collect(payload);
        }
    }
}
