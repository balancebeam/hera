package io.anyway.hera.collector.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ValueFilter;
import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.context.MetricsTraceContext;
import io.anyway.hera.context.MetricsTraceContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by yangzz on 16/9/13.
 */
public class KafkaMetricsHandler implements MetricsHandler,InitializingBean,DisposableBean{

    private Log logger= LogFactory.getLog(KafkaMetricsHandler.class);

    private String servers;

    private int timeout= 30;

    private String clientId;

    private String appId;

    private String database;

    private Map<String,String> tags = new LinkedHashMap<String, String>();

    private Producer<String, String> producer;

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setDatabase(String database) {
        if(database== null || "".equals(database.trim())){
            throw new IllegalArgumentException("database must be not empty.");
        }
        this.database = database;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public void setAppId(String appId) {
        this.appId= appId;
    }

    private ValueFilter filter = new ValueFilter() {
        @Override
        public Object process(Object object, String name, Object value) {
            if (value instanceof BigDecimal || value instanceof Double || value instanceof Float) {
                return new BigDecimal(value.toString());
            }
            return value;
        }
    };

    private SerializerFeature[] features=new SerializerFeature[0];

    @Override
    public void handle(final MetricsQuota type, final Map<String, String> tags, final Map<String, Object> props) {

        Map<String,Object> xprops= new LinkedHashMap<String, Object>(props);
        //获取跟踪链上下文
        MetricsTraceContext ctx= MetricsTraceContextHolder.getMetricsTraceContext();
        if(ctx!= null){
            //设置跟踪链的唯一标识
            xprops.put("traceId",ctx.getTraceId());
            //设置跟踪链栈信息
            xprops.put("trace",ctx.getTraceStack().toString());
            //设置用户请求的地址
            xprops.put("remote",ctx.getRemote());
        }
        //记录采集时间
        xprops.put("timestamp",System.currentTimeMillis());
        //构建维度
        Map<String,String> xtags= new LinkedHashMap<String, String>(this.tags);
        if(tags!= null){
            xtags.putAll(tags);
        }

        if(logger.isDebugEnabled()){
            logger.debug("quota: "+type+" ,tags: "+xtags+" ,props: "+ xprops);
        }

        JSONObject jsonObject= new JSONObject();
        jsonObject.put("database",database);
        jsonObject.put("quota",type.toString());
        jsonObject.put("tags",xtags);
        jsonObject.put("props",xprops);
        try {
            Future<RecordMetadata> future=  producer.send(new ProducerRecord<String, String>("hera-metrics", JSON.toJSONString(jsonObject, filter, features)));
            RecordMetadata metadata= future.get(timeout, TimeUnit.SECONDS);
            if(logger.isDebugEnabled()){
                logger.debug("Send message: {topic:"+metadata.topic()+",partition:"+metadata.partition()+",offset:"+metadata.offset()+"}");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        tags.put("appId",appId);
        tags.put("host",InetAddress.getLocalHost().getHostAddress());
        tags = Collections.unmodifiableMap(tags);
        if(logger.isInfoEnabled()) {
            logger.info("Metrics Tags: " + tags);
        }

        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("client.id",clientId);
        props.put("acks", "0");//异步处理
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<String,String>(props);
        if(logger.isInfoEnabled()){
            logger.info("Metrics Producer: "+producer);
        }
    }

    @Override
    public void destroy() throws Exception {
        if(producer!=null){
            producer.close();
        }
    }
}
