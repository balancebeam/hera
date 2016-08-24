package io.anyway.hera.demo.dao;

import io.anyway.hera.demo.domain.RepositoryDO;

import java.util.List;

/**
 * Created by yangzz on 16/8/17.
 */
public interface RepositoryDao {

    List<RepositoryDO> selectRepository(long id);
}
