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

    @RequestMapping(value="/admin/data/reset", method= RequestMethod.POST)
    public @ResponseBody boolean dataReset(){
        // 删除重建索引
        dao.resetIndex();
        // 同步目录数据
        synchronizeService.asyncAll();
        return true;
    }
}
