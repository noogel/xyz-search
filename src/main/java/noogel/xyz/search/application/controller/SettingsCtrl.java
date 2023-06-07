package noogel.xyz.search.application.controller;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.ModalInfoDto;
import noogel.xyz.search.infrastructure.dto.SearchSettingDto;
import noogel.xyz.search.infrastructure.utils.EnvHelper;
import noogel.xyz.search.service.SearchService;
import noogel.xyz.search.service.SettingService;
import noogel.xyz.search.service.SynchronizeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

@Controller
@Slf4j
public class SettingsCtrl {

    @Resource
    private SettingService settingService;
    @Resource
    private SynchronizeService synchronizeService;

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
        mv.addObject("env", EnvHelper.DEPLOY_ENV);
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

    @RequestMapping(value="/settings/data/sync", method= RequestMethod.POST)
    public @ResponseBody ModalInfoDto dataSync(){
        try {
            // 同步目录数据
            synchronizeService.asyncAll();
            return ModalInfoDto.ofOk("更新索引");
        } catch (Exception ex) {
            log.error("dataSync", ex);
            return ModalInfoDto.ofErr("更新索引：" + ex.getMessage());
        }
    }

    @RequestMapping(value="/settings/data/reset", method= RequestMethod.POST)
    public @ResponseBody ModalInfoDto dataReset(){
        try {
            // 删除重建索引
            synchronizeService.resetIndex();
            // 同步目录数据
            synchronizeService.asyncAll();
            return ModalInfoDto.ofOk("重置索引");
        } catch (Exception ex) {
            log.error("dataReset", ex);
            return ModalInfoDto.ofErr("重置索引：" + ex.getMessage());
        }
    }

    @RequestMapping(value="/settings/connect/testing", method= RequestMethod.POST)
    public @ResponseBody ModalInfoDto connectTesting(SearchSettingDto cfg){
        try{
            boolean res = settingService.connectTesting(cfg);
            if (res) {
                return ModalInfoDto.ofOk("测试连接");
            } else {
                return ModalInfoDto.ofErr("测试连接");
            }
        } catch (Exception ex) {
            log.error("connectTesting {}", cfg, ex);
            return ModalInfoDto.ofErr(ex.getMessage());
        }
    }
}
