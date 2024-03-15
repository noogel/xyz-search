package noogel.xyz.search.application.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.ModalInfoDto;
import noogel.xyz.search.infrastructure.dto.ResourceDownloadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.UrlHelper;
import noogel.xyz.search.service.FileProcessService;
import noogel.xyz.search.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
@Controller
public class FileOpCtrl {

    @Resource
    private SearchService searchService;
    @Resource
    private FileProcessService fileProcessService;

    @RequestMapping(value = "/file/{resId}/download", method = RequestMethod.GET)
    public void fileOp(@PathVariable String resId,
                       HttpServletResponse response) {
        ResourceDownloadDto downloadResource = searchService.getDownloadResource(resId);
        File file = new File(downloadResource.getAbsolutePath());
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        try (InputStream inputStream = new FileInputStream(file)) {
            response.reset();
            response.setContentType("application/octet-stream");
            response.addHeader("Content-Disposition", "attachment; filename="
                    + UrlHelper.ct(downloadResource.getResTitle()));
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                byte[] b = new byte[1024];
                int len;
                while ((len = inputStream.read(b)) > 0) {
                    outputStream.write(b, 0, len);
                }
            }
        } catch (Exception ex) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(ex);
        }
    }

    @RequestMapping(value = "/file/{resId}/delete", method = RequestMethod.GET)
    public @ResponseBody ModalInfoDto fileMarkDelete(@PathVariable String resId) {
        try {
            fileProcessService.fileMarkDelete(resId);
            return ModalInfoDto.ofOk("删除文件");
        } catch (Exception ex) {
            log.error("fileMarkDelete error.", ex);
            return ModalInfoDto.ofErr("删除文件：" + ex.getMessage());
        }
    }
}
