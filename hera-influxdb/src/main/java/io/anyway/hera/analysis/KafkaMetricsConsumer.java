package io.anyway.hera.analysis;

import com.alibaba.fastjson.JSONObject;
import io.anyway.hera.analysis.influxdb.InfluxdbRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;

/**
 * Created by yangzz on 16/9/13.
 */
public class KafkaMetricsConsumer implements InitializingBean,DisposableBean {

    private final static Logger logger = LoggerFactory.getLogger(KafkaMetricsConsumer.class);

    private String servers;

    private String group;

    private String clientId;

    private int timeout= 30;

    private KafkaConsumer<String, String> consumer;

    private InfluxdbRepository influxdbRepository;

    private volatile boolean running= true;

    public void setServers(String servers){
        this.servers= servers;
    }

    public void setGroup(String group){
        this.group= group;
    }

    public void setClientId(String clientId){
        this.clientId= clientId;
    }

    public void setTimeout(int timeout){
        this.timeout= timeout;
    }

    public void setInfluxdbRepository(InfluxdbRepository influxdbRepository) {
        this.influxdbRepository = influxdbRepository;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("group.id", group);
        props.put("client.id",clientId);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<String, String>(props);
        //定义事务处理的topic
        consumer.subscribe(Arrays.asList("hera-metrics"));

        if(logger.isInfoEnabled()){
            logger.info("crete kafka consumer: "+consumer+" ,subscribe topic: hera-metrics");
        }

        final Thread thread= new Thread(){
            @Override
            public void run() {
                for (; running; ) {
                    try {
                        ConsumerRecords<String, String> records = consumer.poll(timeout);
                        for (TopicPartition partition : records.partitions()) {
                            List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
                            for (ConsumerRecord<String, String> each : partitionRecords) {
                                if(logger.isDebugEnabled()){
                                    logger.debug("kafka receive message: "+"{topic:"+each.topic()+",partition:"+partition.partition()+",offset:"+each.offset()+",value:"+each.value()+"}");
                                }
                                Map<String,Object> jsonObject= ( Map<String,Object>)JSONObject.parseObject(each.value(),Map.class);
                                if (logger.isDebugEnabled()) {
                                    logger.debug(jsonObject.toString());
                                }
                                influxdbRepository.send(jsonObject);
                            }
                        }
                    } catch (Throwable e) {
                        logger.error("Consumer message failed ", e);
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void destroy() throws Exception {
        running= false;
        consumer.close();
        if(logger.isInfoEnabled()){
            logger.info("destroy kafka consumer: "+consumer);
        }
    }
}
