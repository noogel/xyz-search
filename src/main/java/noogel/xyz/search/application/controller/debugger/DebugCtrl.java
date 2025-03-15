package noogel.xyz.search.application.controller.debugger;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.CharStreamHelper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/debug")
@Slf4j
public class DebugCtrl {

    @GetMapping("/test")
    public String debugTest() {
        File file = new File("/Users/xyz/Downloads/北大-DeepSeek系列-提示词工程和落地场景-2025.2.22-86页.pdf");
        try (PDDocument doc = Loader.loadPDF(file)) {
            int pages = doc.getNumberOfPages();
            // 获取一个PDFTextStripper文本剥离对象
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            List<String> result = new ArrayList<>();
            for (int page = 1; page <= pages; page++) {
                textStripper.setStartPage(page);
                textStripper.setEndPage(page);
                result.add(textStripper.getText(doc));
            }
            return CharStreamHelper.opt(String.join("\n", result));
        } catch (Exception e) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
    }

    @RequestMapping(value = "/demo", method = RequestMethod.GET)
    public ModelAndView demo() {
        ModelAndView mv = new ModelAndView("demo");
        return mv;
    }

}
