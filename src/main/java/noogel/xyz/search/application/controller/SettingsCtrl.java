package noogel.xyz.search.application.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.application.scheduler.CollectServiceScheduler;
import noogel.xyz.search.infrastructure.dto.ModalInfoDto;
import noogel.xyz.search.infrastructure.dto.SearchSettingDto;
import noogel.xyz.search.infrastructure.utils.EnvHelper;
import noogel.xyz.search.service.SettingService;
import noogel.xyz.search.service.SynchronizeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Slf4j
public class SettingsCtrl {

    @Resource
    private SettingService settingService;
    @Resource
    private SynchronizeService synchronizeService;
    @Resource
    private CollectServiceScheduler collectServiceScheduler;

    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    public ModelAndView getSettings() {
        ModelAndView mv = new ModelAndView("settings");
        mv.addObject("env", EnvHelper.INIT_MODE);
        mv.addObject("configProperties", settingService.query());
        return mv;
    }

    @RequestMapping(value = "/settings", method = RequestMethod.POST)
    public ModelAndView postSettings(SearchSettingDto cfg) {
        ModelAndView mv = new ModelAndView("settings");
        mv.addObject("env", EnvHelper.INIT_MODE);
        try {
            SearchSettingDto dto = settingService.update(cfg);
            mv.addObject("configProperties", dto);
            mv.addObject("result", ModalInfoDto.ofOk("修改配置"));
        } catch (Exception ex) {
            log.error("postSettings err {}", cfg, ex);
            mv.addObject("configProperties", settingService.query());
            mv.addObject("result", ModalInfoDto.ofErr(ex.getMessage()));
        }
        return mv;
    }

    @RequestMapping(value = "/settings/sync/fast", method = RequestMethod.POST)
    public @ResponseBody ModalInfoDto syncFast() {
        try {
            // 同步目录数据
            synchronizeService.asyncDirectories();
            return ModalInfoDto.ofOk("快速更新");
        } catch (Exception ex) {
            log.error("syncFast", ex);
            return ModalInfoDto.ofErr("快速更新：" + ex.getMessage());
        }
    }

    @RequestMapping(value = "/settings/sync/collect", method = RequestMethod.POST)
    public @ResponseBody ModalInfoDto syncCollect() {
        try {
            // 同步目录数据
            collectServiceScheduler.asyncCollectFileIfNotExist();
            return ModalInfoDto.ofOk("收集文件");
        } catch (Exception ex) {
            log.error("syncCollect", ex);
            return ModalInfoDto.ofErr("收集文件：" + ex.getMessage());
        }
    }

    @RequestMapping(value = "/settings/sync/all", method = RequestMethod.POST)
    public @ResponseBody ModalInfoDto syncAll() {
        try {
            // 删除重建索引
            synchronizeService.resetIndex();
            // 同步目录数据
            synchronizeService.asyncDirectories();
            return ModalInfoDto.ofOk("全量更新");
        } catch (Exception ex) {
            log.error("syncAll", ex);
            return ModalInfoDto.ofErr("全量更新：" + ex.getMessage());
        }
    }

}
