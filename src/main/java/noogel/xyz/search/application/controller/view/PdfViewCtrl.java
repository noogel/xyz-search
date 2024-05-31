package noogel.xyz.search.application.controller.view;

import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PdfViewCtrl {


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
