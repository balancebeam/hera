package io.anyway.hera.scheduler;

import io.anyway.hera.common.MetricsCollector;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yangzz on 16/8/19.
 */
public class MetricsCollectorTimer extends TimerTask implements ApplicationListener<ContextRefreshedEvent>,DisposableBean {

    private Timer timer = new Timer();

    private int delay= 1000;

    private int period= 1000;

    private List<MetricsCollector> collectors;

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public void setCollectors(List<MetricsCollector> collectors){
        this.collectors= collectors;
    }

    @Override
    public void run(){
        try {
            for (MetricsCollector each : collectors) {
                each.doCollect();
            }
        }catch(Throwable e){
            //continue;
        }
    }

    @Override
    public void destroy() throws Exception {
        timer.cancel();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        //系统初始化完毕自动采集监控信息
        timer.schedule(this,delay,period);
    }
}
