package io.anyway.hera.scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yangzz on 16/8/19.
 */
public class MetricsTimerProcessor extends TimerTask implements ApplicationListener<ContextRefreshedEvent>,DisposableBean {

    private Log logger= LogFactory.getLog(MetricsTimerProcessor.class);

    private Timer timer = new Timer();

    private int delay= 1000;

    private int period= 1000;

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    private Map<String,MetricsProcessor> timerProcessors= Collections.emptyMap();

    @Override
    public void run(){
        for (MetricsProcessor each: timerProcessors.values()){
            each.doMonitor();
        }
    }

    @Override
    public void destroy() throws Exception {
        timer.cancel();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext ctx= event.getApplicationContext();
        timerProcessors= ctx.getBeansOfType(MetricsProcessor.class);
        logger.info("TimerProcessors: "+timerProcessors);

        timer.schedule(this, delay, period);
        logger.info("Start timer to collect Application parameter");
    }

}
