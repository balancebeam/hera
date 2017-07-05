package io.anyway.hera.collector;

import io.anyway.hera.service.NonMetricService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yangzz on 16/8/19.
 */
import org.springframework.stereotype.Component;

@NonMetricService
@Component
public class MetricCollectorTimer extends TimerTask implements ApplicationListener<ContextRefreshedEvent>,DisposableBean {

    private Timer timer;

    @Value("${hera.timer.delay:60000}")
    private int delay= 5000;

    @Value("${hera.timer.period:60000}")
    private int period= 5000;

    @Autowired
    private List<MetricCollector> collectors;

    @Override
    public void run(){
        for (MetricCollector each : collectors) {
            try {
                each.doCollect();
            }catch(Throwable e){
                //continue;
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        if(timer!=null) {
            timer.cancel();
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(timer== null) {
            timer = new Timer();
            //系统初始化完毕自动采集监控信息
            timer.schedule(this, delay, period);
        }
    }
}
