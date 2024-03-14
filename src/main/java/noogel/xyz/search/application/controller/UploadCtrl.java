package noogel.xyz.search.application.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.dto.dropzone.UploadRespDto;
import noogel.xyz.search.infrastructure.exception.BizException;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import noogel.xyz.search.service.FileProcessService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@RestController
public class UploadCtrl {

    @Resource
    private FileProcessService fileProcessService;
    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

    @RequestMapping(value = "/upload", method = RequestMethod.GET)
    public ModelAndView uploadPage() {
        return new ModelAndView("upload");
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<String> postUploadPage(@RequestParam("file") MultipartFile file) {
        if (StringUtils.isBlank(searchConfig.getApp().getUploadFileDirectory())) {
            throw ExceptionCode.CONFIG_ERROR.throwExc("当前配置未开启");
        }
        if (file.isEmpty()) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc("文件为空");
        }
        try {
            fileProcessService.uploadFile(file, searchConfig.getApp().getUploadFileDirectory());
            return new ResponseEntity<>(JsonHelper.toJson(
                    UploadRespDto.of("Success!")), HttpStatus.OK);
        } catch (Exception exception) {
            if (exception instanceof BizException) {
                log.warn("postUploadPage warn.", exception);
            } else {
                log.error("postUploadPage error.", exception);
            }
            return new ResponseEntity<>(JsonHelper.toJson(
                    UploadRespDto.of(exception.getMessage())), HttpStatus.BAD_REQUEST);
        }
    }

}
