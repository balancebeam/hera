package io.anyway.hera.demo.rest;

import io.anyway.hera.demo.domain.RepositoryDO;
import io.anyway.hera.demo.service.RepositoryService;
import io.anyway.hera.service.ServiceMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


@Controller
@RequestMapping(value = "/rest")
public class RepositoryRest{

    @Autowired
    private RepositoryService repositoryService;

    @RequestMapping(value="/{id}")
    @ResponseBody
    @ServiceMetrics
    public List<RepositoryDO> getRepo(@PathVariable long id)throws Exception {
        repositoryService.f2();
        List<RepositoryDO> result= repositoryService.selectRepository(id);

        return result;
    }
}