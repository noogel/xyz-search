package noogel.xyz.search.application.controller.debugger;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.CharStreamHelper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/debug")
@Slf4j
public class DebugCtrl {

    // URL 格式验证
    private static final String URL_REGEX = "^((http|https)://)?([\\w-]+\\.)+[\\w-]+(/[\\w-?=&/%.#]*)?$";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
    private static final String ENG_REGEX = "^[A-Za-z',.!?]*$";
    private static final Pattern ENG_PATTERN = Pattern.compile(ENG_REGEX);
    private static final String DIG_REGEX = "^[0-9]*$";
    private static final Pattern DIG_PATTERN = Pattern.compile(DIG_REGEX);
    private static final String TWO_HAN_REGEX = "^[\\u4e00-\\u9fa5]{2}$";
    private static final Pattern TWO_HAN_PATTERN = Pattern.compile(TWO_HAN_REGEX);
    // 预编译正则提升性能
    private static final Pattern CH_EN_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5])([A-Za-z0-9])|([A-Za-z0-9])([\\u4e00-\\u9fa5])"
    );

    public static String insertSpaceBetweenChEn(String text) {
        return CH_EN_PATTERN.matcher(text)
                .replaceAll(mr ->
                        mr.group(1) != null ?
                                mr.group(1) + " " + mr.group(2) :
                                mr.group(3) + " " + mr.group(4)
                );
    }

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

}
