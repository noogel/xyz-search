package noogel.xyz.search.service.extension.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Service;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.consts.FileProcessClassEnum;
import noogel.xyz.search.infrastructure.dto.ResRelationInfoDto;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;

@Slf4j
@Service
public class TikaExtensionPointServiceImpl extends AbstractExtensionPointService {
    private final Tika tika = new Tika();

    @Getter
    private final FileProcessClassEnum fileClass = FileProcessClassEnum.OTHER;

    @Getter
    private final Set<FileExtEnum> supportParseFileExtension = Set.of(
            FileExtEnum.MOBI, FileExtEnum.AZW3, FileExtEnum.AZW, FileExtEnum.MP4, FileExtEnum.MKV, FileExtEnum.AVI
    );

    @Override
    public boolean supportFile(String filePath) {
        boolean supportFile = super.supportFile(filePath);
        if (supportFile) {
            FileExtEnum fileExtension = FileHelper.getFileExtension(filePath);
            if (FileExtEnum.MP4.equals(fileExtension)) {
                // 小于 10M 的是视频不索引，扩展点
                if (new File(filePath).length() < 1024 * 1024 * 10) {
                    supportFile = false;
                }
            }
        }
        return supportFile;
    }

    @Nullable
    @Override
    public FileResContentDto parseFile(FileResReadDto resReadDto) {
        File file = resReadDto.genFile();
        ResRelationInfoDto resRel = autoFindRelationInfo(file);
        String title = Optional.ofNullable(resRel).map(ResRelationInfoDto::getTitle).orElse(null);
        
        // 根据文件类型进行特定处理
        FileExtEnum fileExtension = FileHelper.getFileExtension(file.getName());
        String text;
        Metadata metadata = new Metadata();
        
        switch (fileExtension) {
            case MOBI:
            case AZW3:
            case AZW:
                text = parseEbook(file, metadata);
                break;
            case MP4:
            case MKV:
            case AVI:
                text = parseVideo(file, metadata);
                break;
            default:
                text = parseToPlainText(file);
        }
        
        if (StringUtils.isBlank(text)) {
            text = title;
        }

        return FileResContentDto.of(Collections.singletonList(ChapterDto.of("", text)))
                .metaData("metaTitle", title)
                .metaData("format", metadata.get("Content-Type"));
    }

    /**
     * 解析电子书文件
     */
    private String parseEbook(File file, Metadata metadata) {
        try (InputStream stream = new FileInputStream(file)) {
            return tika.parseToString(stream, metadata);
        } catch (Exception e) {
            log.error("parseEbook error {}", file.getAbsolutePath(), e);
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
    }

    /**
     * 解析视频文件
     */
    private String parseVideo(File file, Metadata metadata) {
        try (InputStream stream = new FileInputStream(file)) {
            // 对于视频文件，我们主要提取元数据，文本内容可能较少
            String text = tika.parseToString(stream, metadata);
            // 添加视频文件的基本信息
            return String.format("视频文件: %s\n时长: %s\n分辨率: %sx%s\n格式: %s",
                    file.getName(),
                    metadata.get("xmpDM:duration"),
                    metadata.get("Image Width"),
                    metadata.get("Image Height"),
                    metadata.get("Content-Type"));
        } catch (Exception e) {
            log.error("parseVideo error {}", file.getAbsolutePath(), e);
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
    }

    /**
     * 将文件转换为纯文本
     */
    public String parseToPlainText(File file) {
        try (InputStream stream = new FileInputStream(file)) {
            return tika.parseToString(stream);
        } catch (Exception e) {
            log.error("parseToPlainText error {}", file.getAbsolutePath(), e);
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
    }
}
