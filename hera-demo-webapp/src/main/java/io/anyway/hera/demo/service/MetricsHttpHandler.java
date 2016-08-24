package io.anyway.hera.demo.service;

import io.anyway.hera.common.MetricsResourceHandler;
import io.anyway.hera.common.MetricsType;
import io.anyway.hera.context.MetricsContext;
import io.anyway.hera.context.MetricsContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
@Component
public class MetricsHttpHandler implements MetricsResourceHandler {

    @Override
    public MetricsType getType() {
        return MetricsType.HTTP;
    }

    @Override
    public void handle(final Map<String,String> systemDimension, final Map<String,Object> payload) {
        payload.putAll(systemDimension);
        MetricsContext ctx= MetricsContextHolder.getMetricsContext();
        if(ctx!= null){
            //设置跟踪链的唯一标识
            payload.put("transactionId",ctx.getTransactionId());
            //设置跟踪链栈信息
            payload.put("transactionTrace",ctx.getTransactionTrace());
            //设置用户请求的地址
            payload.put("remote",ctx.getRemote());
        }
        payload.put("category","http");
        System.out.println(payload);
    }
}
