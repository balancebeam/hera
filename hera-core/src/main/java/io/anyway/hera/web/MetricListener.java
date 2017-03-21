package io.anyway.hera.web;

import io.anyway.hera.collector.MetricHandler;
import io.anyway.hera.common.MetricQuota;
import io.anyway.hera.common.MetricUtils;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yangzz on 16/8/17.
 */
public class MetricListener implements HttpSessionListener, HttpSessionActivationListener,
        ServletContextListener, Serializable {

    private AtomicInteger SESSION_COUNT = new AtomicInteger();

    private ConcurrentMap<String, HttpSession> SESSION_MAP_BY_ID = new ConcurrentHashMap<String, HttpSession>();

    private ServletContext servletContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        servletContext= sce.getServletContext();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        SESSION_COUNT.set(0);
        SESSION_MAP_BY_ID.clear();
    }

    @Override
    public void sessionWillPassivate(HttpSessionEvent event) {
        SESSION_COUNT.decrementAndGet();
        removeSession(event.getSession());
    }

    @Override
    public void sessionDidActivate(HttpSessionEvent event) {
        SESSION_COUNT.incrementAndGet();
        addSession(event.getSession());
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        if (session.getAttribute("SESSION_ACTIVATION_KEY")!= null) {
            removeSessionsWithChangedId();
        } else {
            session.setAttribute("SESSION_ACTIVATION_KEY", true);
            SESSION_COUNT.incrementAndGet();
        }
        addSession(session);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        SESSION_COUNT.decrementAndGet();
        removeSession(session);
    }

    private void addSession(final HttpSession session) {
        SESSION_MAP_BY_ID.put(session.getId(), session);
        doMonitor();
    }

    private void removeSession(final HttpSession session) {
        final HttpSession removedSession = SESSION_MAP_BY_ID.remove(session.getId());
        if (removedSession == null) {
            removeSessionsWithChangedId();
        }
        doMonitor();
    }

    private void removeSessionsWithChangedId() {
        for (final Map.Entry<String, HttpSession> entry : SESSION_MAP_BY_ID.entrySet()) {
            final String id = entry.getKey();
            final HttpSession other = entry.getValue();
            if (!id.equals(other.getId())) {
                SESSION_MAP_BY_ID.remove(id);
            }
        }
    }

    private void doMonitor(){
        Map<String,Object> props= new LinkedHashMap<String, Object>();
        props.put("count",SESSION_COUNT.get());
        ApplicationContext ctx= MetricUtils.getWebApplicationContext(servletContext);
        if(ctx!= null) {
            MetricHandler handler = ctx.getBean(MetricHandler.class);
            handler.handle(MetricQuota.SESSION, null, props);
        }
    }

}
