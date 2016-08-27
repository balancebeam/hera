package io.anyway.hera.common;

import com.sohu.idcenter.IdWorker;

/**
 * Created by xiong.j on 2016/7/22.
 */
public class TransactionIdGenerator {
    private final static TransactionIdGenerator transactionIdGenerator = new TransactionIdGenerator();

    private IdWorker idWorker;

    private TransactionIdGenerator() {
        getIdWorker();
    }

    private IdWorker getIdWorker() {
        long idepo = System.currentTimeMillis() - 3600 * 1000L;
        idWorker = new IdWorker(idepo);
        return idWorker;
    }

    public static String next() {
        return ""+transactionIdGenerator.idWorker.getId();
    }
}
