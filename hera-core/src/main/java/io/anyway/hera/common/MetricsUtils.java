package io.anyway.hera.common;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

/**
 * Created by yangzz on 16/11/29.
 */
final public class MetricsUtils {

    private static ThreadLocal<SimpleDateFormat> holder= new ThreadLocal<SimpleDateFormat>();

    public static String formatDate(long time){
        SimpleDateFormat sdf= holder.get();
        if(sdf== null){
            holder.set(sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ms"));
        }
        return sdf.format(new Date(time));
    }

    public static ApplicationContext getWebApplicationContext(ServletContext ctx){
        ApplicationContext applicationContext= WebApplicationContextUtils.getWebApplicationContext(ctx);
        if(applicationContext==null) {
            for (Enumeration each = ctx.getAttributeNames(); each.hasMoreElements(); ) {
                String name = (String) each.nextElement();
                if (ctx.getAttribute(name) instanceof ApplicationContext) {
                    applicationContext= (ApplicationContext) ctx.getAttribute(name);
                    break;
                }
            }
        }
        return applicationContext;
    }
}
