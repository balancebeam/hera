package io.anyway.hera.jdbc;

import io.anyway.hera.collector.MetricHandler;
import io.anyway.hera.common.MetricQuota;
import io.anyway.hera.context.MetricTraceContextHolder;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by yangzz on 16/8/17.
 */
class JdbcWrapper {

    private final static Log logger= LogFactory.getLog(JdbcWrapper.class);

    private MetricHandler handler;

    static Map<String,String> DATASOURCE_CONFIG_METADATA= new LinkedHashMap<String, String>();

    static {
        String[] metadata= {
            "url",
            "initialSize",
            "maxActive",
            "maxWait",
            "maxIdle",
            "minIdle"
        };
        for(String each: metadata){
            DATASOURCE_CONFIG_METADATA.put(each,each);
        }
    }

    private final Map<String,Object> DATASOURCE_CONFIG_PROPERTIES = new LinkedHashMap<String, Object>();

    private final AtomicInteger ACTIVE_CONNECTION_COUNT = new AtomicInteger();
    private final AtomicInteger HOLDED_CONNECTION_COUNT = new AtomicInteger();
    private final AtomicLong USED_CONNECTION_COUNT = new AtomicLong();
    private final Map<Integer, StackTraceElement[]> HOLD_CONNECTION_INFORMATIONS = new ConcurrentHashMap<Integer, StackTraceElement[]>();

    private static final int MAX_USED_CONNECTION_INFORMATIONS = 500;

    JdbcWrapper(MetricHandler handler) {
        this.handler= handler;
    }

    /**
     * Statement | PreparedStatement InvocationHandler
     */
    private class StatementInvocationHandler implements InvocationHandler {
        private String requestName;
        private final Statement statement;

        StatementInvocationHandler(String query, Statement statement) {
            super();
            assert statement != null;

            this.requestName = query;
            this.statement = statement;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final String methodName = method.getName();
            if (isEqualsMethod(methodName, args)) {
                return statement.equals(args[0]);
            } else if (isHashCodeMethod(methodName, args)) {
                return statement.hashCode();
            } else if (methodName.startsWith("execute")) {
                if (isFirstArgAString(args)) {
                    requestName = (String) args[0];
                }
                requestName = String.valueOf(requestName);
                return doExecute(requestName, statement, method, args);
            } else if ("addBatch".equals(methodName) && isFirstArgAString(args)) {
                requestName = (String) args[0];
            }
            return method.invoke(statement, args);
        }

        private boolean isFirstArgAString(Object[] args) {
            return args != null && args.length > 0 && args[0] instanceof String;
        }
    }

    boolean isConnectionInformationsEnabled(){
        return true;
    }

    /**
     * Connection InvocationHandler
     */
    private class ConnectionInvocationHandler implements InvocationHandler {
        private final Connection connection;
        private boolean alreadyClosed;

        ConnectionInvocationHandler(Connection connection) {
            super();
            assert connection != null;
            this.connection = connection;
        }

        void init() {
            if (isConnectionInformationsEnabled()
                    && HOLD_CONNECTION_INFORMATIONS.size() < MAX_USED_CONNECTION_INFORMATIONS) {
                HOLD_CONNECTION_INFORMATIONS.put(System.identityHashCode(connection),
                       Thread.currentThread().getStackTrace());
            }
            HOLDED_CONNECTION_COUNT.incrementAndGet();
            USED_CONNECTION_COUNT.incrementAndGet();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final String methodName = method.getName();
            if (isEqualsMethod(methodName, args)) {
                return areConnectionsEquals(args[0]);
            } else if (isHashCodeMethod(methodName, args)) {
                return connection.hashCode();
            }
            try {
                Object result = method.invoke(connection, args);
                if (result instanceof Statement) {
                    final String requestName;
                    if ("prepareStatement".equals(methodName) || "prepareCall".equals(methodName)) {
                        requestName = (String) args[0];
                    } else {
                        requestName = null;
                    }
                    result = createStatementProxy(requestName, (Statement) result);
                }
                return result;
            } finally {
                if ("close".equals(methodName) && !alreadyClosed) {
                    HOLDED_CONNECTION_COUNT.decrementAndGet();
                    HOLD_CONNECTION_INFORMATIONS
                            .remove(System.identityHashCode(connection));
                    alreadyClosed = true;
                }
            }
        }

        private boolean areConnectionsEquals(Object object) {
            if (Proxy.isProxyClass(object.getClass())) {
                final InvocationHandler invocationHandler = Proxy.getInvocationHandler(object);
                if (invocationHandler instanceof DelegatingInvocationHandler) {
                    final DelegatingInvocationHandler d = (DelegatingInvocationHandler) invocationHandler;
                    if (d.getDelegate() instanceof ConnectionInvocationHandler) {
                        final ConnectionInvocationHandler c = (ConnectionInvocationHandler) d
                                .getDelegate();
                        return connection.equals(c.connection);
                    }
                }
            }
            return connection.equals(object);
        }
    }

    private abstract static class AbstractInvocationHandler<T>
            implements InvocationHandler, Serializable {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("all")
        private final T proxiedObject;

        AbstractInvocationHandler(T proxiedObject) {
            super();
            this.proxiedObject = proxiedObject;
        }

        T getProxiedObject() {
            return proxiedObject;
        }
    }

    private static class DelegatingInvocationHandler implements InvocationHandler, Serializable {
        private static final long serialVersionUID = 7515240588169084785L;
        @SuppressWarnings("all")
        private final InvocationHandler delegate;

        DelegatingInvocationHandler(InvocationHandler delegate) {
            super();
            this.delegate = delegate;
        }

        InvocationHandler getDelegate() {
            return delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                return delegate.invoke(proxy, method, args);
            } catch (final InvocationTargetException e) {
                if (e.getTargetException() != null) {
                    throw e.getTargetException();
                }
                throw e;
            }
        }
    }

    Object doExecute(String requestName, Statement statement, Method method, Object[] args)
            throws IllegalAccessException, InvocationTargetException {
        assert requestName != null;
        assert statement != null;
        assert method != null;

        if (requestName.startsWith("explain ")) {
            ACTIVE_CONNECTION_COUNT.incrementAndGet();
            try {
                return method.invoke(statement, args);
            } finally {
                ACTIVE_CONNECTION_COUNT.decrementAndGet();
            }
        }
        Map<String,Object> props= new LinkedHashMap<String, Object>();
        final long beginTime = System.currentTimeMillis();
        //设置开始时间
        props.put("beginTime",beginTime);
        try {
            ACTIVE_CONNECTION_COUNT.incrementAndGet();
            return method.invoke(statement, args);
        } catch (final InvocationTargetException e) {
            if (e.getCause() instanceof SQLException) {
                final int errorCode = ((SQLException) e.getCause()).getErrorCode();
                if (errorCode >= 20000 && errorCode < 30000) {
                    //记录执行sql的出错信息
                    Throwable ex= e.getCause();
                    Map<String,String> xtags= new LinkedHashMap<String,String>();
                    xtags.put("class",ex.getClass().getSimpleName());
                    xtags.put("quota", MetricQuota.SQL.toString());
                    Map<String,Object> xprops= new LinkedHashMap<String,Object>();
                    xprops.put("message",ex.getMessage());
                    xprops.put("beginTime",System.currentTimeMillis());
                    handler.handle(MetricQuota.EXCEPTION,xtags,xprops);
                    if(MetricTraceContextHolder.getMetricTraceContext()!= null){
                        MetricTraceContextHolder.getMetricTraceContext().addException(ex);
                    }
                }
            }
            throw e;
        } finally {
            ACTIVE_CONNECTION_COUNT.decrementAndGet();
            long endTime= System.currentTimeMillis();
            //设置调用方法名称
            props.put("sql",requestName);
            //记录sql语句的长度大小
            props.put("length",requestName.length());
            //记录执行的时间
            props.put("duration",endTime-beginTime);
            //发送监控记录
            handler.handle(MetricQuota.SQL, null, props);
        }
    }

    /**
     * 创建代理数据源对象
     * @param name
     * @param dataSource
     * @return
     */
    DataSource createDataSourceProxy(String name, final DataSource dataSource) {
        assert dataSource != null;
        pullDataSourceConfigProperties(name,dataSource);
        final InvocationHandler invocationHandler = new AbstractInvocationHandler<DataSource>(
                dataSource) {
            private static final long serialVersionUID = 1L;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object result = method.invoke(dataSource, args);
                if (result instanceof Connection) {
                    result = createConnectionProxy((Connection) result);
                }
                return result;
            }
        };
        return createProxy(dataSource, invocationHandler);
    }

    /**
     * 拉取数据源的配置属性
     * @param name
     * @param dataSource
     */
    private void pullDataSourceConfigProperties(String name, DataSource dataSource){
        DATASOURCE_CONFIG_PROPERTIES.clear();
        for(Map.Entry<String,String> each: DATASOURCE_CONFIG_METADATA.entrySet()){
            try {
                Object result= BeanUtils.getProperty(dataSource,each.getValue());
                if(result!= null) {
                    DATASOURCE_CONFIG_PROPERTIES.put(each.getKey(), result);
                }
            }
            catch (Throwable e){
                logger.warn("DataSource "+name+" miss property:"+ each);
            }
        }
        logger.info("DataSourceWrapper properties: "+ DATASOURCE_CONFIG_PROPERTIES);
    }

    /**
     * 创建代理Connection对象
     * @param connection
     * @return
     */
    Connection createConnectionProxy(Connection connection) {
        assert connection != null;
        final ConnectionInvocationHandler invocationHandler = new ConnectionInvocationHandler(
                connection);
        final Connection result;
//        if (jonas) {
//            result = createProxy(connection, invocationHandler,
//                    Arrays.asList(new Class<?>[] { Connection.class }));
//        } else {
            result = createProxy(connection, invocationHandler);
       // }
        if (result != connection) { // NOPMD
            invocationHandler.init();
        }
        return result;
    }

    /**
     * 创建Statement|PreparedStatement对象
     * @param query
     * @param statement
     * @return
     */
    Statement createStatementProxy(String query, Statement statement) {
        assert statement != null;
        final InvocationHandler invocationHandler = new StatementInvocationHandler(query,
                statement);
        return createProxy(statement, invocationHandler);
    }

    boolean isEqualsMethod(Object methodName, Object[] args) {
        return "equals" == methodName && args != null && args.length == 1; // NOPMD
    }

    boolean isHashCodeMethod(Object methodName, Object[] args) {
        return "hashCode" == methodName && (args == null || args.length == 0);
    }

    int getHoldedConnectionCount() {
        return HOLDED_CONNECTION_COUNT.get();
    }

    int getActiveConnectionCount() {
        return ACTIVE_CONNECTION_COUNT.get();
    }

    long getUsedConnectionCount() {
        return USED_CONNECTION_COUNT.get();
    }

    int getMaxActive() {
        if(DATASOURCE_CONFIG_PROPERTIES.containsKey("maxActive")){
            return Integer.parseInt(DATASOURCE_CONFIG_PROPERTIES.get("maxActive").toString());
        }
        return -1;
    }

    int getMaxWait(){
        if(DATASOURCE_CONFIG_PROPERTIES.containsKey("maxWait")){
            return Integer.parseInt(DATASOURCE_CONFIG_PROPERTIES.get("maxWait").toString());
        }
        return -1;
    }
    String getUrl(){
        if(DATASOURCE_CONFIG_PROPERTIES.containsKey("url")){
            return (String)DATASOURCE_CONFIG_PROPERTIES.get("url");
        }
        return "------";
    }

    int getInitialSize(){
        if(DATASOURCE_CONFIG_PROPERTIES.containsKey("initialSize")){
            return Integer.parseInt(DATASOURCE_CONFIG_PROPERTIES.get("initialSize").toString());
        }
        return -1;
    }
    int getMaxIdle(){
        if(DATASOURCE_CONFIG_PROPERTIES.containsKey("maxIdle")){
            return Integer.parseInt(DATASOURCE_CONFIG_PROPERTIES.get("maxIdle").toString());
        }
        return -1;
    }
    int getMinIdle(){
        if(DATASOURCE_CONFIG_PROPERTIES.containsKey("minIdle")){
            return Integer.parseInt(DATASOURCE_CONFIG_PROPERTIES.get("minIdle").toString());
        }
        return -1;
    }

    Map<String,Object> getDBConfigProperties(){
        return DATASOURCE_CONFIG_PROPERTIES;
    }

    Collection<StackTraceElement[]> getBlockingStackTraces() {
        return HOLD_CONNECTION_INFORMATIONS.values();
    }

    static <T> T createProxy(T object, InvocationHandler invocationHandler) {
        return createProxy(object, invocationHandler, null);
    }

    static <T> T createProxy(T object, InvocationHandler invocationHandler,
                             List<Class<?>> interfaces) {
        if (isProxyAlready(object)) {
            return object;
        }
        InvocationHandler ih = new DelegatingInvocationHandler(invocationHandler);
        return JdbcWrapperHelper.createProxy(object, ih, interfaces);
    }

    private static boolean isProxyAlready(Object object) {
        return Proxy.isProxyClass(object.getClass()) && Proxy.getInvocationHandler(object)
                .getClass().getName().equals(DelegatingInvocationHandler.class.getName());
    }

}
