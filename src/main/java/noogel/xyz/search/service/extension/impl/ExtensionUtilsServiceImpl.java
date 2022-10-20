package noogel.xyz.search.service.extension.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.ResRelationInfoDto;
import noogel.xyz.search.infrastructure.utils.AuthorHelper;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.XmlHelper;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Service
@Slf4j
public class ExtensionUtilsServiceImpl implements ExtensionUtilsService {

    @Override
    public boolean supportFileExtension(Set<String> supportExtension, File file) {
        if (file.isDirectory()) {
            return false;
        }
        String fileExtension = FileHelper.getFileExtension(file.getAbsolutePath());
        return supportExtension.contains(fileExtension);
    }

    @Nullable
    @Override
    public ResRelationInfoDto autoFindRelationInfo(File file) {
        ResRelationInfoDto dto = new ResRelationInfoDto();
        String parent = file.getParent();
        File metaFile = new File(String.format("%s/metadata.opf", parent));
        if (metaFile.exists()) {
            try {
                Charset charset = FileHelper.detectCharset(metaFile);
                Path metaPath = Paths.get(metaFile.toURI());
                String text = Files.readString(metaPath, charset);
                JsonNode jsonNode = XmlHelper.parseXml(text);
                StringBuilder resTitle = new StringBuilder();
                Optional.ofNullable(jsonNode).map(t -> t.get("metadata")).map(t -> t.get("title"))
                        .map(JsonNode::toString).map(AuthorHelper::format).ifPresent(resTitle::append);
                Optional.ofNullable(jsonNode).map(t -> t.get("metadata")).map(t -> t.get("creator"))
                        .ifPresent(t -> {
                            if (!resTitle.isEmpty()) {
                                resTitle.append(" - ");
                            }
                            List<String> authors = new ArrayList<>();
                            Consumer<JsonNode> fn = (ti) -> Optional.of(ti).map(k -> k.get("")).map(JsonNode::toString)
                                    .map(AuthorHelper::format).ifPresent(authors::add);
                            if (t.isArray()) {
                                t.forEach(fn);
                            } else {
                                fn.accept(t);
                            }
                            resTitle.append(String.join(",", authors));
                        });
                if (!resTitle.isEmpty()) {
                    resTitle.append(".").append(FileHelper.getFileExtension(file.getName()));
                    dto.setTitle(resTitle.toString());
                }
                Optional.ofNullable(jsonNode).map(JsonNode::toString).ifPresent(dto::setMetaContent);
                log.debug("autoFindRelationInfo parse metadata.opf {} {}", file.getAbsoluteFile(), jsonNode);
                return dto;
            } catch (Exception ex) {
                log.error("autoFindRelationInfo metaFile {}", file.getAbsoluteFile());
            }
        }
        return null;
    }
}
