package io.anyway.hera.demo.service.impl;

import io.anyway.hera.demo.dao.RepositoryDao;
import io.anyway.hera.demo.domain.RepositoryDO;
import io.anyway.hera.demo.service.RepositoryService;
import io.anyway.hera.service.ServiceMetrics;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Random;

/**
 * Created by yangzz on 16/7/19.
 */

@Service
public class RepositoryServiceImpl implements RepositoryService {

    Log logger= LogFactory.getLog(RepositoryServiceImpl.class);

    @Autowired
    private RepositoryDao repositoryDao;

    @Autowired
    private DataSource dataSource;

    @Override
    public List<RepositoryDO> selectRepository(long id) {
        return repositoryDao.selectRepository(id);
    }

    @Override
    @ServiceMetrics
    public void f2() {
        logger.info("----------------invoke-------f2");
        try {
            Connection connection= dataSource.getConnection();
            Statement statement= connection.createStatement();
            ResultSet rs= statement.executeQuery("select * from t_repository");
            while(rs.next()){
                System.out.println("rs="+rs.getString(1));
            }
            rs.close();
            statement.close();

            Thread.sleep(new Random().nextInt(7000));
            //Thread.sleep(70000);
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
