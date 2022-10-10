package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.config.CommonsConstConfig;
import noogel.xyz.search.infrastructure.dto.*;
import noogel.xyz.search.infrastructure.dao.ElasticSearchFtsDao;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.ResourceModel;
import noogel.xyz.search.infrastructure.utils.DateTimeHelper;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.HTMLTemplateHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Resource
    private ElasticSearchFtsDao dao;

    public SearchResultShowDto search(SearchQueryDto query) {
        SearchResultDto result = dao.search(query);
        SearchResultShowDto showDto = new SearchResultShowDto();
        PagingDto pagingDto = PagingDto.of(query, result.getSize());
        showDto.setPaging(pagingDto);
        showDto.setData(result.getData().stream().map(t -> {
            ResourceSimpleDto page = new ResourceSimpleDto();
            page.setResId(t.getResId());
            page.setResName(t.getResName());
            page.setResSize(FileHelper.formatFileSize(t.getResSize()));
            page.setModifiedAt(DateTimeHelper.tsToDt(t.getModifiedAt()));
            return page;
        }).collect(Collectors.toList()));
        return showDto;
    }

    public ResourcePageDto searchByResId(String resId, String search) {
        ResourceHighlightHitsDto dto = dao.searchByResId(resId, search);
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(Objects.isNull(dto), "资源不存在");
        String highlightHtml = HTMLTemplateHelper.render("highlight.html",
                Collections.singletonMap("highlight", dto.getHighlights()));
        ResourceModel t = dto.getResource();
        ResourcePageDto page = new ResourcePageDto();
        page.setResId(t.getResId());
        page.setResName(t.getResName());
        page.setResSize(FileHelper.formatFileSize(t.getResSize()));
        page.setModifiedAt(DateTimeHelper.tsToDt(t.getModifiedAt()));
        page.setResPath(t.calculateFullPath());
        page.setResType(t.getResType());
        page.setHighlightHtml(highlightHtml);
        File file = new File(t.calculateFullPath());
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        try {
            String contentType = Files.probeContentType(file.toPath());
            page.setContentType(contentType);
            String fileExtension = FileHelper.getFileExtension(file.getAbsolutePath());
            page.setSupportView(CommonsConstConfig.SUPPORT_VIEW_EXT.contains(fileExtension));
        } catch (IOException e) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
        return page;
    }

    public String getResourcePath(String resId) {
        return dao.findByResId(resId).calculateFullPath();
    }
}
