package noogel.xyz.search.application.controller;

import noogel.xyz.search.infrastructure.dao.ElasticSearchFtsDao;
import noogel.xyz.search.service.SynchronizeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
public class AdminCtrl {

    @Resource
    private ElasticSearchFtsDao dao;
    @Resource
    private SynchronizeService synchronizeService;

    @RequestMapping(value="/admin/es/index/reset", method= RequestMethod.GET)
    public @ResponseBody boolean reset(){
        return dao.resetIndex();
    }

    @RequestMapping(value="/admin/es/data/sync", method= RequestMethod.GET)
    public @ResponseBody boolean sync(){
        synchronizeService.asyncAll();
        return true;
    }

}
