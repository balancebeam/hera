package io.anyway.hera.httpclient;

import java.util.Collection;

/**
 * Created by yangzz on 17/2/21.
 */
public interface HttpClientStackTraceRepository {

    Collection<StackTraceElement[]> getBlockingStackTrace();
}
