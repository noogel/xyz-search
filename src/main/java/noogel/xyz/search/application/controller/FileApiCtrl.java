package noogel.xyz.search.application.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.service.FileDbService;

/**
 * 文件API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileApiCtrl {

    @Resource
    private FileDbService fileDbService;
    
    /**
     * 获取文件信息
     */
    @GetMapping("/{resId}")
    public ResponseEntity<Map<String, Object>> getFileInfo(@PathVariable String resId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询文件信息
            Optional<FileResReadDto> fileOpt = fileDbService.findByResIdFilterState(resId, FileStateEnum.INDEXED);
            if (fileOpt.isPresent()) {
                FileResReadDto file = fileOpt.get();
                result.put("id", file.getFieldId());
                result.put("name", file.getName());
                result.put("path", file.getDir() + "/" + file.getName());
                result.put("size", file.getSize());
                result.put("modifiedAt", file.getModifiedAt());
                result.put("type", file.getType());
            } else {
                result.put("error", "文件不存在");
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取文件信息失败", e);
            result.put("error", "获取文件信息失败");
            return ResponseEntity.internalServerError().body(result);
        }
    }
} 