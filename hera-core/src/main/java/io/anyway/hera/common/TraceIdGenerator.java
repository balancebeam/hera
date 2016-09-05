package io.anyway.hera.common;

import com.sohu.idcenter.IdWorker;

/**
 * Created by xiong.j on 2016/7/22.
 */
public class TraceIdGenerator {
    private final static TraceIdGenerator TRACE_ID_GENERATOR = new TraceIdGenerator();

    private IdWorker idWorker;

    private TraceIdGenerator() {
        getIdWorker();
    }

    private IdWorker getIdWorker() {
        long idepo = System.currentTimeMillis() - 3600 * 1000L;
        idWorker = new IdWorker(idepo);
        return idWorker;
    }

    public static String next() {
        return ""+ TRACE_ID_GENERATOR.idWorker.getId();
    }
}
