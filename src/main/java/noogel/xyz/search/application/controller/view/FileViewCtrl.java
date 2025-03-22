package noogel.xyz.search.application.controller.view;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.ResourceDownloadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.ImageHelper;
import noogel.xyz.search.service.SearchService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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

    @RequestMapping(value = "/file/view/thumbnail/{resId}", method = RequestMethod.GET)
    public void fileViewThumbnail(@PathVariable String resId,
                                  HttpServletResponse response) {
        ResourceDownloadDto downloadResource = searchService.getDownloadResource(resId);
        File file = new File(downloadResource.getAbsolutePath());
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        try {
            byte[] smallImageByteList = ImageHelper.genThumbnailToByteArray(file.getAbsolutePath(), 600, null);
            response.reset();
            String contentType = FileHelper.getContentType(file);
            response.setContentType(contentType);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                outputStream.write(smallImageByteList, 0, smallImageByteList.length);
            }
        } catch (Exception ex) {
            log.error("fileViewThumbnail error.", ex);
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(ex);
        }
    }

    @RequestMapping(value = "/file/view/text/{resId}", method = RequestMethod.GET)
    public String fileViewText(@PathVariable String resId,
                               HttpServletResponse response) {
        return searchService.getResourceContent(resId);
    }

}
