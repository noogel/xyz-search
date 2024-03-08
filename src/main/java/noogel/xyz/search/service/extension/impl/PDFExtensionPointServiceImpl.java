package noogel.xyz.search.service.extension.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.ResRelationInfoDto;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileResHelper;
import noogel.xyz.search.service.extension.ExtensionPointService;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class PDFExtensionPointServiceImpl implements ExtensionPointService {

    private static final Set<String> SUPPORT = Set.of("pdf");

    @Resource
    private ExtensionUtilsService extensionUtilsService;

    /**
     * 读取pdf中文字信息(全部)
     */
    public static List<ChapterDto> readPdfChapters(File inputFile) {
        List<ChapterDto> resp = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(inputFile)) {
            int pages = doc.getNumberOfPages();
            // 获取一个PDFTextStripper文本剥离对象
            PDFTextStripper textStripper = new PDFTextStripper();
            for (int page = 1; page <= pages; page += 10) {
                int endPage = Math.min(pages, page + 9);
                textStripper.setStartPage(page);
                textStripper.setEndPage(endPage);
                String content = textStripper.getText(doc).replace("\n", "").replace("  ", "").trim();
                String chapter = String.format("第%s-%s页", page, endPage);
                resp.add(ChapterDto.of(chapter, content));
            }
        } catch (Exception e) {
            if (e instanceof InvalidPasswordException) {
                log.warn("parsePdf InvalidPasswordException {}", inputFile.getAbsoluteFile());
                resp.add(ChapterDto.of(null, e.getMessage()));
            }
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
        // PDF 全为空，需要重置
        if (resp.size() > 1 && resp.stream().allMatch(t -> StringUtils.isBlank(t.getText()))) {
            resp.clear();
            resp.add(ChapterDto.of(null, ""));
        }
        return resp;
    }

    @Override
    public boolean supportFile(File file) {
        return extensionUtilsService.supportFileExtension(SUPPORT, file);
    }

    @Nullable
    @Override
    public FileResContentDto parseFile(FileResReadDto resReadDto) {
        File file = resReadDto.genFile();
        List<ChapterDto> chapters = readPdfChapters(file);
        ResRelationInfoDto resRel = extensionUtilsService.autoFindRelationInfo(file);
        String title = Optional.ofNullable(resRel).map(ResRelationInfoDto::getTitle).orElse(null);
        // 补偿内容
        if (chapters.size() == 1) {
            chapters.stream().findFirst().filter(t -> StringUtils.isBlank(t.getText())).ifPresent(t -> {
                String metaContent = Optional.ofNullable(resRel)
                        .map(ResRelationInfoDto::getMetaContent)
                        .filter(StringUtils::isNotBlank).orElse(file.getName());
                t.setChapter(metaContent);
            });
        }
        return FileResContentDto.of(chapters, title);
    }

}
