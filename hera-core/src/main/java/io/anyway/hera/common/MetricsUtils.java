package io.anyway.hera.common;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import java.text.SimpleDateFormat;
import java.util.*;

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

    public static Class<?>[] getInterfaces(Class<?> clazz){
        return getInterfaces(clazz,new Class<?>[0]);
    }

    public static Class<?>[] getInterfaces(Class<?> clazz,Class<?> ...interfaces){
        Set<Class<?>> idx= new HashSet<Class<?>>();
        for(;clazz!=null && clazz!=Object.class;){
            if(clazz.getInterfaces().length>0){
                idx.addAll(Arrays.asList(clazz.getInterfaces()));
            }
            clazz= clazz.getSuperclass();
        }
        if(interfaces!=null && interfaces.length>0){
            idx.addAll(Arrays.asList(interfaces));
        }
        return idx.toArray(new Class<?>[idx.size()]);
    }
}
