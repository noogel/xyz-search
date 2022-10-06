package noogel.xyz.search.application.controller;

import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.service.SettingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;

@Controller
public class SettingCtrl {

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;
    @Resource
    private SettingService settingService;

    @RequestMapping(value="/admin/setting", method= RequestMethod.GET)
    public @ResponseBody
    SearchPropertyConfig.SearchConfig getSettings(){
        return searchConfig;
    }

    @RequestMapping(value="/admin/setting", method= RequestMethod.POST)
    public @ResponseBody boolean postSettings(@RequestParam Map<String,String> cfg){
        return settingService.update(cfg);
    }

}
