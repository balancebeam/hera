package io.anyway.hera.common;

import com.sohu.idcenter.IdWorker;
import io.anyway.hera.service.NonMetricService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetAddress;

@NonMetricService
public class IdGenerator implements InitializingBean {

    private final static IdGenerator ID_GENERATOR = new IdGenerator();

    private IdWorker idWorker;

    @Value("${hera.appId}")
    private String appId;

    public static String next() {
        return ""+ ID_GENERATOR.idWorker.getId();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String id= InetAddress.getLocalHost()+":"+appId;
        long idepoch= System.identityHashCode(id);
        ID_GENERATOR.idWorker= new IdWorker(idepoch);
    }
}
