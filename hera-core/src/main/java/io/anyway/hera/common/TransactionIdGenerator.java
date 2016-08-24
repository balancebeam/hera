package io.anyway.hera.common;

import com.sohu.idcenter.IdWorker;

import java.util.Random;
import java.util.UUID;

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
//        long workerId= new Random().nextInt((int)(-1L ^ (-1L << 5L)));
//        long datacenterId= UUID.randomUUID().getMostSignificantBits();
        long idepo = System.currentTimeMillis() - 3600 * 1000L;
        idWorker = new IdWorker(idepo);
        return idWorker;
    }

    public static String next() {
        return ""+transactionIdGenerator.idWorker.getId();
    }
}
