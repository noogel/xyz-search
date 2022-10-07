package noogel.xyz.search.application.controller;

import noogel.xyz.search.infrastructure.dao.ElasticSearchFtsDao;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.model.ResourceModel;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import noogel.xyz.search.service.SynchronizeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
public class TestCtrl {

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

    @RequestMapping(value="/admin/es/data/diff", method= RequestMethod.GET)
    public @ResponseBody SearchResultDto showOldRes(@RequestParam(required = false) String resId){
        ResourceModel res = dao.findByResId(resId);
        String resPathHash = MD5Helper.getMD5(res.getResPath());
        Long taskOpAt = res.getTaskOpAt();
        return dao.searchOldRes(resPathHash, taskOpAt);
    }

    @RequestMapping(value="/admin/es/data/delete", method= RequestMethod.GET)
    public @ResponseBody boolean delete(@RequestParam(required = false) String resId){
        return dao.deleteByResId(resId);
    }

}
