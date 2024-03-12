package noogel.xyz.search.application.controller;

import jakarta.annotation.Resource;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@RestController
public class UploadCtrl {

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

    @RequestMapping(value = "/upload", method = RequestMethod.GET)
    public ModelAndView uploadPage() {
        ModelAndView mv = new ModelAndView("upload");
        return mv;
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<String> postUploadPage(@RequestParam("file") MultipartFile file) {
        // Code to save the file to a database or disk
        if (StringUtils.isBlank(searchConfig.getApp().getUploadFileDirectory())) {
            throw ExceptionCode.CONFIG_ERROR.throwExc("当前配置未开启");
        }
        uploadFile(file);
        return new ResponseEntity<>("Success!", HttpStatus.OK);
    }
    public void uploadFile(MultipartFile file) {
        try {
            if(file.isEmpty()) {
                throw ExceptionCode.FILE_ACCESS_ERROR.throwExc("文件为空");
            }
            File upDir = new File(searchConfig.getApp().getUploadFileDirectory());
            if (!upDir.exists()) {
                upDir.mkdirs();
            }
            Path destination = Paths.get(searchConfig.getApp().getUploadFileDirectory())
                    .resolve(Objects.requireNonNull(file.getOriginalFilename())).normalize().toAbsolutePath();
            if (!destination.startsWith(upDir.getAbsolutePath())) {
                throw ExceptionCode.FILE_ACCESS_ERROR.throwExc("目录错误");
            }
            Files.copy(file.getInputStream(), destination);
        } catch(IOException e) {
            throw ExceptionCode.RUNTIME_ERROR.throwExc(e.getMessage());
        }
    }
}
