package io.anyway.hera.common;

import com.sohu.idcenter.IdWorker;
import io.anyway.hera.service.NonMetricService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetAddress;

@NonMetricService
@Component
public class IdGenerator implements InitializingBean {

    private final static IdGenerator ID_GENERATOR = new IdGenerator();

    private IdWorker idWorker;

    @Value("${hera.appId}")
    private String appId;

    @Autowired
    private ApplicationContext applicationContext;

    public static String next() {
        return ""+ ID_GENERATOR.idWorker.getId();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String id= InetAddress.getLocalHost()+":"+appId;
        long idepoch= System.identityHashCode(id);
        ID_GENERATOR.idWorker= new IdWorker(idepoch);
        MetricUtils.applicationContext= applicationContext;
    }
}
