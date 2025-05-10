package noogel.xyz.search.application.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.application.scheduler.CollectServiceScheduler;
import noogel.xyz.search.infrastructure.dto.ModalInfoDto;
import noogel.xyz.search.infrastructure.dto.SearchSettingDto;
import noogel.xyz.search.infrastructure.dto.settings.DataStorageSettingDto;
import noogel.xyz.search.infrastructure.dto.settings.FileDirectorySettingDto;
import noogel.xyz.search.infrastructure.dto.settings.NotificationIntegrationSettingDto;
import noogel.xyz.search.infrastructure.dto.settings.SearchAiSettingDto;
import noogel.xyz.search.infrastructure.dto.settings.SettingsDto;
import noogel.xyz.search.infrastructure.utils.EnvHelper;
import noogel.xyz.search.service.SettingService;
import noogel.xyz.search.service.SynchronizeService;

@Controller
@Slf4j
public class SettingsCtrl {

    @Resource
    private SettingService settingService;
    @Resource
    private SynchronizeService synchronizeService;
    @Resource
    private CollectServiceScheduler collectServiceScheduler;

    /**
     * 主设置页面 - 新版标签式界面
     */
    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    public ModelAndView getSettings() {
        ModelAndView mv = new ModelAndView("settings_new");
        mv.addObject("env", EnvHelper.INIT_MODE);
        SearchSettingDto searchSettingDto = settingService.query();
        SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(searchSettingDto);
        mv.addObject("settings", settingsDto);
        return mv;
    }
    
    /**
     * 保存所有设置 - 新版标签式界面
     */
    @RequestMapping(value = "/settings", method = RequestMethod.POST)
    public ModelAndView postSettings(@ModelAttribute SettingsDto settings) {
        ModelAndView mv = new ModelAndView("settings_new");
        mv.addObject("env", EnvHelper.INIT_MODE);
        
        try {
            // 验证配置
            String error = settings.validate();
            if (error != null) {
                mv.addObject("settings", settings);
                mv.addObject("result", ModalInfoDto.ofErr(error));
                return mv;
            }
            
            // 转换配置并保存
            SearchSettingDto searchSettingDto = settings.toSearchSettingDto();
            searchSettingDto = settingService.update(searchSettingDto);
            
            // 转换回新格式
            SettingsDto updatedSettings = SettingsDto.fromSearchSettingDto(searchSettingDto);
            mv.addObject("settings", updatedSettings);
            mv.addObject("result", ModalInfoDto.ofOk("修改配置成功"));
        } catch (Exception ex) {
            log.error("postSettings err {}", settings, ex);
            mv.addObject("settings", settings);
            mv.addObject("result", ModalInfoDto.ofErr(ex.getMessage()));
        }
        
        return mv;
    }
    
    /**
     * 单独更新文件与目录设置
     */
    @RequestMapping(value = "/settings/file-directory", method = RequestMethod.POST)
    public ModelAndView updateFileDirectorySetting(@ModelAttribute FileDirectorySettingDto setting) {
        ModelAndView mv = new ModelAndView("settings_new");
        mv.addObject("env", EnvHelper.INIT_MODE);
        
        try {
            // 验证配置
            String error = setting.validate();
            if (error != null) {
                SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(settingService.query());
                settingsDto.setFileDirectorySetting(setting);
                mv.addObject("settings", settingsDto);
                mv.addObject("result", ModalInfoDto.ofErr(error));
                return mv;
            }
            
            // 获取当前配置并更新
            SearchSettingDto currentSettings = settingService.query();
            SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(currentSettings);
            settingsDto.setFileDirectorySetting(setting);
            
            // 保存配置
            SearchSettingDto updatedSettingDto = settingService.update(settingsDto.toSearchSettingDto());
            SettingsDto updatedSettings = SettingsDto.fromSearchSettingDto(updatedSettingDto);
            
            mv.addObject("settings", updatedSettings);
            mv.addObject("result", ModalInfoDto.ofOk("文件与目录设置更新成功"));
        } catch (Exception ex) {
            log.error("updateFileDirectorySetting err {}", setting, ex);
            SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(settingService.query());
            settingsDto.setFileDirectorySetting(setting);
            mv.addObject("settings", settingsDto);
            mv.addObject("result", ModalInfoDto.ofErr(ex.getMessage()));
        }
        
        return mv;
    }
    
    /**
     * 单独更新搜索与AI服务设置
     */
    @RequestMapping(value = "/settings/search-ai", method = RequestMethod.POST)
    public ModelAndView updateSearchAiSetting(@ModelAttribute SearchAiSettingDto setting) {
        ModelAndView mv = new ModelAndView("settings_new");
        mv.addObject("env", EnvHelper.INIT_MODE);
        
        try {
            // 验证配置
            String error = setting.validate();
            if (error != null) {
                SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(settingService.query());
                settingsDto.setSearchAiSetting(setting);
                mv.addObject("settings", settingsDto);
                mv.addObject("result", ModalInfoDto.ofErr(error));
                return mv;
            }
            
            // 获取当前配置并更新
            SearchSettingDto currentSettings = settingService.query();
            SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(currentSettings);
            settingsDto.setSearchAiSetting(setting);
            
            // 保存配置
            SearchSettingDto updatedSettingDto = settingService.update(settingsDto.toSearchSettingDto());
            SettingsDto updatedSettings = SettingsDto.fromSearchSettingDto(updatedSettingDto);
            
            mv.addObject("settings", updatedSettings);
            mv.addObject("result", ModalInfoDto.ofOk("搜索与AI服务设置更新成功"));
        } catch (Exception ex) {
            log.error("updateSearchAiSetting err {}", setting, ex);
            SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(settingService.query());
            settingsDto.setSearchAiSetting(setting);
            mv.addObject("settings", settingsDto);
            mv.addObject("result", ModalInfoDto.ofErr(ex.getMessage()));
        }
        
        return mv;
    }
    
    /**
     * 单独更新数据存储配置设置
     */
    @RequestMapping(value = "/settings/data-storage", method = RequestMethod.POST)
    public ModelAndView updateDataStorageSetting(@ModelAttribute DataStorageSettingDto setting) {
        ModelAndView mv = new ModelAndView("settings_new");
        mv.addObject("env", EnvHelper.INIT_MODE);
        
        try {
            // 验证配置
            String error = setting.validate();
            if (error != null) {
                SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(settingService.query());
                settingsDto.setDataStorageSetting(setting);
                mv.addObject("settings", settingsDto);
                mv.addObject("result", ModalInfoDto.ofErr(error));
                return mv;
            }
            
            // 获取当前配置并更新
            SearchSettingDto currentSettings = settingService.query();
            SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(currentSettings);
            settingsDto.setDataStorageSetting(setting);
            
            // 保存配置
            SearchSettingDto updatedSettingDto = settingService.update(settingsDto.toSearchSettingDto());
            SettingsDto updatedSettings = SettingsDto.fromSearchSettingDto(updatedSettingDto);
            
            mv.addObject("settings", updatedSettings);
            mv.addObject("result", ModalInfoDto.ofOk("数据存储配置更新成功"));
        } catch (Exception ex) {
            log.error("updateDataStorageSetting err {}", setting, ex);
            SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(settingService.query());
            settingsDto.setDataStorageSetting(setting);
            mv.addObject("settings", settingsDto);
            mv.addObject("result", ModalInfoDto.ofErr(ex.getMessage()));
        }
        
        return mv;
    }
    
    /**
     * 单独更新通知与集成设置
     */
    @RequestMapping(value = "/settings/notification", method = RequestMethod.POST)
    public ModelAndView updateNotificationSetting(@ModelAttribute NotificationIntegrationSettingDto setting) {
        ModelAndView mv = new ModelAndView("settings_new");
        mv.addObject("env", EnvHelper.INIT_MODE);
        
        try {
            // 验证配置
            String error = setting.validate();
            if (error != null) {
                SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(settingService.query());
                settingsDto.setNotificationIntegrationSetting(setting);
                mv.addObject("settings", settingsDto);
                mv.addObject("result", ModalInfoDto.ofErr(error));
                return mv;
            }
            
            // 获取当前配置并更新
            SearchSettingDto currentSettings = settingService.query();
            SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(currentSettings);
            settingsDto.setNotificationIntegrationSetting(setting);
            
            // 保存配置
            SearchSettingDto updatedSettingDto = settingService.update(settingsDto.toSearchSettingDto());
            SettingsDto updatedSettings = SettingsDto.fromSearchSettingDto(updatedSettingDto);
            
            mv.addObject("settings", updatedSettings);
            mv.addObject("result", ModalInfoDto.ofOk("通知与集成设置更新成功"));
        } catch (Exception ex) {
            log.error("updateNotificationSetting err {}", setting, ex);
            SettingsDto settingsDto = SettingsDto.fromSearchSettingDto(settingService.query());
            settingsDto.setNotificationIntegrationSetting(setting);
            mv.addObject("settings", settingsDto);
            mv.addObject("result", ModalInfoDto.ofErr(ex.getMessage()));
        }
        
        return mv;
    }

    /**
     * 同步相关操作
     */
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
