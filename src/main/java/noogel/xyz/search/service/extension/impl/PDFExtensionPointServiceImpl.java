package noogel.xyz.search.service.extension.impl;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.TaskDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.ResourceModel;
import noogel.xyz.search.service.extension.ExtensionPointService;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.File;
import java.util.Set;

@Service
@Slf4j
public class PDFExtensionPointServiceImpl implements ExtensionPointService {

    private static final Set<String> SUPPORT = Set.of("pdf");

    @Resource
    private ExtensionUtilsService extensionUtilsService;

    @Override
    public boolean supportFile(File file) {
        return extensionUtilsService.supportFileExtension(SUPPORT, file);
    }

    @Nullable
    @Override
    public ResourceModel parseFile(File file, TaskDto task) {
        String text = readPdf(file);
        text = text.replace("\n", "").replace("  ", "");
        return ResourceModel.buildBaseInfo(file, text, task);
    }

    /**
     * 读取pdf中文字信息(全部)
     */
    public static String readPdf(File inputFile) {
        try (PDDocument doc = PDDocument.load(inputFile)) {
            // 获取一个PDFTextStripper文本剥离对象
            PDFTextStripper textStripper = new PDFTextStripper();
            return textStripper.getText(doc);
        } catch (Exception e) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
    }
}
