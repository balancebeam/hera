package io.anyway.hera.jdbc;

import io.anyway.hera.common.MetricsType;
import io.anyway.hera.common.MetricsManager;
import io.anyway.hera.common.MetricsCollector;
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
public class DataSourceCollector implements BeanPostProcessorWrapper,ServletContextAware,MetricsCollector {

    private Log logger= LogFactory.getLog(DataSourceCollector.class);

    private Map<String,JdbcWrapper> jdbcWrappers= new HashMap<String, JdbcWrapper>();

    private List<String> excludedDatasources;

    private ServletContext servletContext;

    /**
     * Connection泄露的跟踪有效包路径
     * @param leakInterestTracePackages
     */
    public void setLeakInterestTracePackages(String leakInterestTracePackages){
        if(!StringUtils.isEmpty(leakInterestTracePackages)){
            List<String> leakPackages= Arrays.asList(leakInterestTracePackages.split(","));
            LeakConnectionInformations.LEAK_INTEREST_TRACE_PACKAGES = leakPackages;
            logger.info("LEAK_INTEREST_TRACE_PACKAGES: "+leakPackages);
        }
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
            if(!jdbcWrappers.containsKey(beanName)) {
                final DataSource dataSource = (DataSource) bean;
                JdbcWrapper jdbcWrapper = new JdbcWrapper(servletContext);
                jdbcWrappers.put(beanName, jdbcWrapper);
                final DataSource result = jdbcWrapper.createDataSourceProxy(beanName, dataSource);
                logger.info("Spring datasource wrapped: " + beanName);
                return result;
            }
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
    public void doCollect() {
        for (Map.Entry<String,JdbcWrapper> each: jdbcWrappers.entrySet()){
            JdbcWrapper jdbcWrapper= each.getValue();
            Map<String,Object> payload= new LinkedHashMap<String, Object>();
            payload.put("name",each.getKey());
            payload.put("maxActive",jdbcWrapper.getMaxActive());
            payload.put("maxWait",jdbcWrapper.getMaxWait());
            payload.put("activeCount",jdbcWrapper.getActiveConnectionCount());
            payload.put("usedCount",jdbcWrapper.getUsedConnectionCount());
            payload.put("leakCount",jdbcWrapper.getHoldedConnectionCount());
            List<Map<String,Object>> traceList= new LinkedList<Map<String, Object>>();
            for (LeakConnectionInformations info: jdbcWrapper.getHoldedConnectionInformationsList()){
                Map<String,Object> hash= new LinkedHashMap<String, Object>();
                hash.put("openingTime",info.getOpeningTime());
                hash.put("stackTrace",info.getOpeningStackTrace().toString());
                traceList.add(hash);
            }
            payload.put("leakTraceList",traceList.toString());
            payload.put("timestamp",MetricsManager.toLocalDate(System.currentTimeMillis()));
            MetricsManager.collect(MetricsType.JDBC,payload);
        }
    }
}
