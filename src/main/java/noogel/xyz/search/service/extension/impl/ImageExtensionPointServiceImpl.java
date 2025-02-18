package noogel.xyz.search.service.extension.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.consts.FileProcessClassEnum;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileToBase64;
import noogel.xyz.search.infrastructure.utils.HttpClient;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class ImageExtensionPointServiceImpl extends AbstractExtensionPointService {

    @Getter
    private final FileProcessClassEnum fileClass = FileProcessClassEnum.IMAGE;

    @Getter
    private final Set<FileExtEnum> supportParseFileExtension = Set.of(
            FileExtEnum.JPEG, FileExtEnum.JPG, FileExtEnum.PNG, FileExtEnum.WEBP, FileExtEnum.BMP,
            FileExtEnum.PSD, FileExtEnum.TIFF, FileExtEnum.JFIF, FileExtEnum.SGI
    );
    // todo "heif", "heic",

    @Resource
    private ConfigProperties configProperties;

    @Override
    public boolean supportFile(String filePath) {
        // 开启 ocr 并且是图片格式
        return super.supportFile(filePath) && configProperties.getApp().supportPaddleOcr();
    }

    @Nullable
    @Override
    public FileResContentDto parseFile(FileResReadDto resReadDto) {
        String url = configProperties.getApp().getPaddleOcr().getUrl();
        Integer timeout = configProperties.getApp().getPaddleOcr().getTimeout();
        if (Objects.isNull(timeout) || timeout == 0) {
            timeout = 60_000;
        }
        File file = resReadDto.genFile();
        Path path = Paths.get(file.toURI());
        try {
            byte[] bytes = Files.readAllBytes(path);
            String conversion = FileToBase64.conversion(bytes);
            // data = {"images": [cv2_to_base64(img)]}
            var data = Collections.singletonMap("images", Collections.singletonList(conversion));
            String json = JsonHelper.toJson(data);
            String string = HttpClient.doPost(url, json, timeout);
            ArrayNode jsonNode = (ArrayNode) JsonHelper.fromJson(string, ObjectNode.class).get("results");
            jsonNode = (ArrayNode) jsonNode.get(0);
            StringBuilder resp = new StringBuilder();
            for (int i = 0; i < jsonNode.size(); i++) {
                JsonNode result = jsonNode.get(i);
                ObjectNode result1 = (ObjectNode) result;
                String text = result1.get("text").asText();
                resp.append(" ");
                resp.append(text);
            }
            log.info("process image {}", resReadDto.calFilePath());
            return FileResContentDto.of(Collections.singletonList(ChapterDto.of("", resp.toString())));
        } catch (Exception ex) {
            log.error("ImageExtensionPointServiceImpl error {}", path, ex);
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(ex);
        }
    }
}
