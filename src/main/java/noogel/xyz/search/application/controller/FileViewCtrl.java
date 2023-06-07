package noogel.xyz.search.application.controller;

import noogel.xyz.search.infrastructure.dto.ResourceDownloadDto;
import noogel.xyz.search.infrastructure.dto.ResourcePageDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.UrlHelper;
import noogel.xyz.search.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Controller
public class FileViewCtrl {

    @Resource
    private SearchService searchService;

    @RequestMapping(value = "/file", method = RequestMethod.GET)
    public @ResponseBody
    ResourcePageDto file(@RequestParam(required = false) String search,
                         @RequestParam(required = false) String resId) {
        return searchService.searchByResId(resId, search);
    }

    @RequestMapping(value = "/file/{resId}", method = RequestMethod.GET)
    public void fileOp(@PathVariable String resId,
                       @RequestParam(required = false, defaultValue = "view") String type,
                       HttpServletResponse response) {
        ResourceDownloadDto downloadResource = searchService.getDownloadResource(resId);
        File file = new File(downloadResource.getAbsolutePath());
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        try (InputStream inputStream = new FileInputStream(file)) {
            response.reset();
            if ("download".equals(type)) {
                response.setContentType("application/octet-stream");
                response.addHeader("Content-Disposition", "attachment; filename="
                        + UrlHelper.ct(downloadResource.getResTitle()));
            } else {
                String contentType = FileHelper.getContentType(file);
                response.setContentType(contentType);
            }
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

    /**
     * PDF 专用展示页面
     *
     * @param file
     * @return
     */
    @RequestMapping(value = "/pdf.js/web/view", method = RequestMethod.GET)
    public ModelAndView filePdfView(@RequestParam(required = true) String file) {
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.startsWith("/file/"), "资源不存在");
        return new ModelAndView("pdf/viewer");
    }

}
