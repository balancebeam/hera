package io.anyway.hera.demo.service;


import io.anyway.hera.demo.domain.RepositoryDO;

import java.util.List;

/**
 * Created by yangzz on 16/7/19.
 */
public interface RepositoryService {

    List<RepositoryDO> selectRepository(long id);

    void f2();
}
