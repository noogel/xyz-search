package noogel.xyz.search.application.controller;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;

/**
 * 上传相关API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadApiCtrl {

    @Resource
    private ConfigProperties configProperties;
    
    /**
     * 获取上传目录配置状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getUploadFolderStatus() {
        Map<String, Object> result = new HashMap<>();
        
        String uploadDirectory = null;
        if (configProperties.getApp() != null) {
            uploadDirectory = configProperties.getApp().getUploadFileDirectory();
        }
        
        boolean isConfigured = StringUtils.isNotBlank(uploadDirectory);
        
        result.put("configured", isConfigured);
        if (isConfigured) {
            result.put("directory", uploadDirectory);
        }
        
        return ResponseEntity.ok(result);
    }
} 