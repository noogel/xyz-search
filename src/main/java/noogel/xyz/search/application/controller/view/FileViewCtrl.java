package noogel.xyz.search.application.controller.view;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.ResourceDownloadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@RestController
@Slf4j
public class FileViewCtrl {
    @Resource
    private SearchService searchService;

    @RequestMapping(value = "/file/view/{resId}", method = RequestMethod.GET)
    public void fileView(@PathVariable String resId,
                       HttpServletResponse response) {
        ResourceDownloadDto downloadResource = searchService.getDownloadResource(resId);
        File file = new File(downloadResource.getAbsolutePath());
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        try (InputStream inputStream = new FileInputStream(file)) {
            response.reset();
            String contentType = FileHelper.getContentType(file);
            response.setContentType(contentType);
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

    @RequestMapping(value = "/file/view/{resId}/thumbnail", method = RequestMethod.GET)
    public void fileViewThumbnail(@PathVariable String resId,
                       HttpServletResponse response) {
        ResourceDownloadDto downloadResource = searchService.getDownloadResource(resId);
        File file = new File(downloadResource.getAbsolutePath());
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        try (InputStream inputStream = new FileInputStream(file)) {
            response.reset();
            String contentType = FileHelper.getContentType(file);
            response.setContentType(contentType);
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

}
