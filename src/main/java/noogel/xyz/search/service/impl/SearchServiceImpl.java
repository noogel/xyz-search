package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.CommonsConstConfig;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.dao.elastic.ElasticDao;
import noogel.xyz.search.infrastructure.dto.*;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.elastic.FileEsModel;
import noogel.xyz.search.infrastructure.utils.DateTimeHelper;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.HTMLTemplateHelper;
import noogel.xyz.search.service.SearchService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Resource
    private ElasticDao dao;
    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

    @Override
    public SearchResultShowDto pageSearch(SearchQueryDto query) {
        SearchResultDto result = dao.search(query);
        SearchResultShowDto showDto = new SearchResultShowDto();
        PagingDto pagingDto = PagingDto.of(query, result.getSize());
        showDto.setPaging(pagingDto);
        showDto.setBreadcrumb(BreadcrumbDto.of(query.getResId(), query.getRelativeResDir()));
        showDto.setData(result.getData().stream().map(t -> {
            ResourceSimpleDto page = new ResourceSimpleDto();
            page.setResId(t.getResId());
            page.setResTitle(t.getResTitle());
            page.calculateSearchableResTitle();
            page.setResSize(String.format("%s | %s", FileHelper.formatFileSize(t.getResSize()),
                    FileHelper.formatFileSize(t.getTextSize())));
            page.setModifiedAt(DateTimeHelper.tsToDt(t.getModifiedAt()));
            return page;
        }).collect(Collectors.toList()));
        return showDto;
    }

    @Override
    public OPDSResultShowDto opdsSearch(SearchQueryDto query) {
        SearchResultDto result = dao.search(query);
        OPDSResultShowDto showDto = new OPDSResultShowDto();
        showDto.setSize(Math.toIntExact(result.getSize()));
        showDto.setExactSize(result.isExactSize());
        showDto.setData(result.getData().stream().map(t -> {
            File file = new File(t.calculateAbsolutePath());
            if (!file.exists()) {
                return null;
            }
            String contentType = FileHelper.getContentType(file);

            OPDSItemShowDto dto = new OPDSItemShowDto();
            dto.setResId(t.getResId());
            dto.setResName(t.getResName());
            dto.setResTitle(t.getResTitle());
            dto.setResSize(t.getResSize());
            dto.setModifiedAt(t.getModifiedAt());
            dto.setSearchableText(t.getSearchableText());
            dto.setResDir(t.getResDir());
            dto.setContentType(contentType);
            return dto;
        }).filter(Objects::nonNull).collect(Collectors.toList()));
        return showDto;
    }

    @Override
    public ResourcePageDto searchByResId(String resId, String search) {
        ResourceHighlightHitsDto dto = dao.searchByResId(resId, search);
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(Objects.isNull(dto), "资源不存在");
        String highlightHtml = HTMLTemplateHelper.render("highlight.html",
                Collections.singletonMap("highlight", dto.getHighlights()));
        FileEsModel t = dto.getResource();
        ResourcePageDto page = new ResourcePageDto();
        page.setResId(t.getResId());
        page.setResTitle(t.getResTitle());
        page.calculateSearchableResTitle();
        page.setResSize(FileHelper.formatFileSize(t.getResSize()));
        page.setModifiedAt(DateTimeHelper.tsToDt(t.getModifiedAt()));
        page.setRelativeResPath(t.calculateRelativePath(searchConfig.getApp().getSearchDirectories()));
        page.setRelativeResDir(t.calculateRelativeDir(searchConfig.getApp().getSearchDirectories()));
        page.setResType(t.getResType());
        page.setHighlightHtml(highlightHtml);
        File file = new File(t.calculateAbsolutePath());
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        String contentType = FileHelper.getContentType(file);
        page.setContentType(contentType);
        String fileExtension = FileHelper.getFileExtension(file.getAbsolutePath());
        page.setSupportView(CommonsConstConfig.SUPPORT_VIEW_EXT.contains(fileExtension));
        return page;
    }

    @Override
    public List<ResourceSimpleDto> searchByResHash(String resHash) {
        List<FileEsModel> models = dao.findByResHash(resHash);
        return models.stream().map(t -> {
            ResourceSimpleDto page = new ResourceSimpleDto();
            page.setResId(t.getResId());
            page.setResTitle(t.getResTitle());
            page.calculateSearchableResTitle();
            page.setResSize(String.format("%s | %s", FileHelper.formatFileSize(t.getResSize()),
                    FileHelper.formatFileSize(t.getTextSize())));
            page.setModifiedAt(DateTimeHelper.tsToDt(t.getModifiedAt()));
            return page;
        }).collect(Collectors.toList());
    }

    @Override
    public ResourceDownloadDto getDownloadResource(String resId) {
        ResourceDownloadDto dto = new ResourceDownloadDto();
        FileEsModel res = dao.findByResId(resId);
        dto.setResId(resId);
        dto.setResTitle(res.getResTitle());
        dto.setAbsolutePath(res.calculateAbsolutePath());
        dto.setResDir(res.getResDir());
        return dto;
    }
}
