package noogel.xyz.search.application.controller;

import noogel.xyz.search.infrastructure.dao.ElasticSearchFtsDao;
import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.service.SynchronizeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Collections;

@Controller
public class AdminCtrl {

    @Resource
    private ElasticSearchFtsDao dao;
    @Resource
    private SynchronizeService synchronizeService;

    @RequestMapping(value="/admin/es/data/test", method= RequestMethod.GET)
    public @ResponseBody
    ResourceHighlightHitsDto test(){
        return dao.searchByResHash("ad500deb06a07e4cecad980ed6699c5d", "测试");
    }

    @RequestMapping(value="/admin/es/index/delete", method= RequestMethod.GET)
    public @ResponseBody boolean delete(){
        return dao.deleteIndexIfExist();
    }

    @RequestMapping(value="/admin/es/data/sync", method= RequestMethod.GET)
    public @ResponseBody boolean sync(){
        synchronizeService.async(Collections.singletonList("/home/xyz/TestSearch"));
        return true;
    }

}
