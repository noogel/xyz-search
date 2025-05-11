package noogel.xyz.search.service.extension.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.consts.FileProcessClassEnum;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;

@Service
@Slf4j
public class WordExtensionPointServiceImpl extends AbstractExtensionPointService {

    @Getter
    private final FileProcessClassEnum fileClass = FileProcessClassEnum.OFFICE;

    @Getter
    private final Set<FileExtEnum> supportParseFileExtension = Set.of(FileExtEnum.DOC, FileExtEnum.DOCX);

    @Nullable
    @Override
    public FileResContentDto parseFile(FileResReadDto resReadDto) {
        File file = resReadDto.genFile();
        try (InputStream inputStream = new FileInputStream(file)) {
            try (XWPFDocument doc = new XWPFDocument(inputStream)) {
                // 获取所有段落
                List<XWPFParagraph> paragraphs = doc.getParagraphs();
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph paragraph : paragraphs) {
                    String text = paragraph.getText();
                    sb.append(text);
                    sb.append("\n");
                }
                return FileResContentDto.of(Collections.singletonList(ChapterDto.of("", sb.toString())));
            }
        } catch (OLE2NotOfficeXmlFileException e) {
            log.warn("处理 office 文件失败,跳过 {}", file.getAbsoluteFile(), e);
            return FileResContentDto.of(Collections.singletonList(ChapterDto.of("", "")));
        } catch (Exception e) {
            log.error("处理 office 文件失败", e);
            throw ExceptionCode.RUNTIME_ERROR.throwExc("处理 office 文件失败");
        }
    }
}
