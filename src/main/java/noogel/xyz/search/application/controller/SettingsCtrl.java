package noogel.xyz.search.application.controller;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.ModalInfoDto;
import noogel.xyz.search.infrastructure.dto.SearchSettingDto;
import noogel.xyz.search.infrastructure.utils.EnvHelper;
import noogel.xyz.search.service.SettingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

@Controller
@Slf4j
public class SettingsCtrl {

    @Resource
    private SettingService settingService;

    @RequestMapping(value="/settings", method= RequestMethod.GET)
    public ModelAndView getSettings(){
        ModelAndView mv = new ModelAndView("settings");
        mv.addObject("env", EnvHelper.DEPLOY_ENV);
        mv.addObject("searchConfig", settingService.query());
        return mv;
    }

    @RequestMapping(value="/settings", method= RequestMethod.POST)
    public ModelAndView postSettings(SearchSettingDto cfg){
        ModelAndView mv = new ModelAndView("settings");
        try {
            SearchSettingDto dto = settingService.update(cfg);
            mv.addObject("searchConfig", dto);
            mv.addObject("result", ModalInfoDto.ofOk("修改配置"));
        } catch (Exception ex) {
            log.error("postSettings err {}", cfg, ex);
            mv.addObject("searchConfig", settingService.query());
            mv.addObject("result", ModalInfoDto.ofErr(ex.getMessage()));
        }
        return mv;
    }

}
