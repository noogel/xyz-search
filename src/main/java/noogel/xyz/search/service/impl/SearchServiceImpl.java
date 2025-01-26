package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.SearchPropertiesConfig;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.dao.elastic.ElasticDao;
import noogel.xyz.search.infrastructure.dto.*;
import noogel.xyz.search.infrastructure.dto.page.PageViewExtEnum;
import noogel.xyz.search.infrastructure.dto.page.ResourcePageDto;
import noogel.xyz.search.infrastructure.dto.page.ResourceSimpleDto;
import noogel.xyz.search.infrastructure.dto.page.SearchResultShowDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.elastic.FileEsModel;
import noogel.xyz.search.infrastructure.utils.DateTimeHelper;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.HTMLTemplateHelper;
import noogel.xyz.search.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Resource
    private ElasticDao elasticDao;
    @Resource
    private SearchPropertiesConfig.SearchConfig searchConfig;

    @Override
    public SearchResultShowDto pageSearch(SearchQueryDto query) {
        SearchResultDto result = elasticDao.search(query);
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
        SearchResultDto result = elasticDao.search(query);
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
        ResourceHighlightHitsDto dto = elasticDao.searchByResId(resId, search);
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
        page.setResType(t.getResType());
        page.setHighlightHtml(highlightHtml);
        File file = new File(t.calculateAbsolutePath());
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        String contentType = FileHelper.getContentType(file);
        page.setContentType(contentType);
        FileExtEnum fileExtension = FileHelper.getFileExtension(file.getAbsolutePath());
        Optional<PageViewExtEnum> pageViewExtEnum = PageViewExtEnum.find(fileExtension);
        page.setSupportView(pageViewExtEnum.map(PageViewExtEnum::supportView).orElse(false));
        page.setViewUrl(pageViewExtEnum.map(l -> l.calViewUrl(t.getResId())).orElse(""));
        page.setSupportThumbnailView(pageViewExtEnum.map(PageViewExtEnum::isThumbnail).orElse(false));
        page.setThumbnailViewUrl(pageViewExtEnum.map(l -> l.calThumbnailUrl(t.getResId())).orElse("#"));
        page.setDownloadUrl(PageViewExtEnum.downloadUrl(t.getResId()));
        page.setDirViewUrl(PageViewExtEnum.dirViewUrl(t.getResId(), t.calculateRelativeDir(searchConfig.getApp().getSearchDirectories())));
        page.setResTextSnippet(genResTextSnippet(t.getSearchableText()));
        return page;
    }

    private String genResTextSnippet(String txt) {
        if (StringUtils.isBlank(txt)) {
            return "";
        }
        if (StringUtils.isNotBlank(txt) && txt.length() > 500) {
            return txt.substring(0, 500) + "...";
        }
        return txt;
    }

    @Override
    public List<ResourceSimpleDto> searchByResHash(String resHash) {
        List<FileEsModel> models = elasticDao.findByResHash(resHash);
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
        FileEsModel res = elasticDao.findByResId(resId);
        dto.setResId(resId);
        dto.setResTitle(res.getResTitle());
        dto.setAbsolutePath(res.calculateAbsolutePath());
        dto.setResDir(res.getResDir());
        return dto;
    }
}
