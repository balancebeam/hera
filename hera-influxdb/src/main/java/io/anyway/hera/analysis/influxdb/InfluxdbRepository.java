package io.anyway.hera.analysis.influxdb;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.SystemPropertyUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by yangzz on 16/12/2.
 */
public class InfluxdbRepository implements InitializingBean,DisposableBean{

    private String server;

    private String username;

    private String password;

    private String database;

    private String retention;

    private InfluxDB influxDB;

    private Map<String,List<String>> blacklist= Collections.EMPTY_MAP;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setRetention(String retention) {
        this.retention = retention;
    }

    public void setServer(String server){
        this.server= server;
    }

    public void setBlacklist(Map<String, List<String>> blacklist) {
        this.blacklist = blacklist;
    }

    public void send(Map<String,Object> jsonObject){
        String quota= (String)jsonObject.get("quota");
        Point.Builder builder = Point.measurement(quota);

        Map<String,String> tags= (Map<String,String>)jsonObject.get("tags");
        if(tags!= null){
            builder.tag(tags);
        }
        Map<String,Object> props= (Map<String,Object>)jsonObject.get("props");
        Long timestamp= (Long)props.remove("timestamp");
        if(timestamp== null){
            timestamp= System.currentTimeMillis();
        }
        builder.time(timestamp, TimeUnit.MILLISECONDS);
        List<String> excludeProps= blacklist.get(quota);
        if(!CollectionUtils.isEmpty(excludeProps)){
            for(String each: excludeProps){
                props.remove(each);
            }
        }
        builder.fields(props);
        influxDB.write(database, retention, builder.build());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        influxDB = InfluxDBFactory.connect(server, username, password);
        //influxDB.createDatabase(dbname);
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() throws Exception {
    }
}
