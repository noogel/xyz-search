package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.dto.*;
import noogel.xyz.search.infrastructure.dto.api.SearchQueryApiDto;
import noogel.xyz.search.infrastructure.dto.api.SearchResultApiDto;
import noogel.xyz.search.infrastructure.dto.api.SearxngResourceApiDto;
import noogel.xyz.search.infrastructure.dto.page.PageViewExtEnum;
import noogel.xyz.search.infrastructure.dto.page.ResourcePageDto;
import noogel.xyz.search.infrastructure.dto.page.ResourceSimpleDto;
import noogel.xyz.search.infrastructure.dto.page.SearchResultShowDto;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto;
import noogel.xyz.search.infrastructure.dto.repo.RandomSearchDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;
import noogel.xyz.search.infrastructure.repo.FullTextSearchRepo;
import noogel.xyz.search.infrastructure.utils.DateTimeHelper;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.HTMLTemplateHelper;
import noogel.xyz.search.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Resource
    private FullTextSearchRepo fullTextSearchRepo;
    @Resource
    private ConfigProperties configProperties;

    @Override
    public SearchResultShowDto pageSearch(SearchQueryDto query) {
        SearchResultDto result = Objects.equals(query.getRandomScore(), true) ? randomSearch(query) : commonSearch(query);
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
                    FileHelper.formatFileSize(t.getContentSize())));
            page.setModifiedAt(DateTimeHelper.tsToDt(t.getModifiedAt()));
            return page;
        }).collect(Collectors.toList()));
        return showDto;
    }

    private SearchResultDto commonSearch(SearchQueryDto query) {
        CommonSearchDto searchDto = new CommonSearchDto();
        searchDto.setSearchQuery(query.getSearch());
        searchDto.setResId(query.getResId());
        searchDto.setDirPrefix(query.getResDirPrefix());
        Optional.ofNullable(query.getResDirPrefix()).filter(StringUtils::isNotBlank)
                .ifPresent(l -> {
                    searchDto.setDirPrefix(l);
                    searchDto.setResId(null);
                });
        Optional.ofNullable(query.getResSize()).filter(StringUtils::isNotBlank)
                .map(CommonSearchDto.Field::of).ifPresent(searchDto::setResSize);
        Optional.ofNullable(query.getResType()).filter(l -> !CollectionUtils.isEmpty(l))
                .ifPresent(searchDto::setResTypeList);
        Optional.ofNullable(query.getModifiedAt()).filter(StringUtils::isNotBlank)
                .map(CommonSearchDto.Field::of).ifPresent(searchDto::setModifiedAt);
        Optional.of(CommonSearchDto.Paging.of(query.getLimit(), query.getPage()))
                .ifPresent(searchDto::setPaging);
        Optional.ofNullable(query.getOrder()).map(l -> CommonSearchDto.OrderBy.of(l.getField(), l.isAscOrder()))
                .ifPresent(searchDto::setOrder);
        SearchResultDto result = fullTextSearchRepo.commonSearch(searchDto);
        return result;
    }

    private SearchResultDto randomSearch(SearchQueryDto query) {
        RandomSearchDto searchDto = new RandomSearchDto();
        searchDto.setLimit(query.getLimit());
        SearchResultDto result = fullTextSearchRepo.randomSearch(searchDto);
        return result;
    }

    @Override
    public SearchResultApiDto apiSearch(SearchQueryApiDto query) {
        CommonSearchDto searchDto = new CommonSearchDto();
        searchDto.setSearchQuery(query.getSearch());
        SearchResultDto result = fullTextSearchRepo.commonSearch(searchDto);
        SearchResultApiDto showDto = new SearchResultApiDto();
        showDto.setQuery(query.getSearch());
        showDto.setResults(result.getData().stream().map(t -> {
            SearxngResourceApiDto page = new SearxngResourceApiDto();
            page.setResId(t.getResId());
            page.setTitle(t.getResTitle());
            page.setContent(t.getContent().substring(0, Math.min(100, t.getContent().length())));
            page.setUrl(PageViewExtEnum.textUrl(t.getResId()));
            return page;
        }).collect(Collectors.toList()));
        return showDto;
    }

    @Override
    public OPDSResultShowDto opdsSearch(SearchQueryDto query) {
        CommonSearchDto searchDto = new CommonSearchDto();
        searchDto.setSearchQuery(query.getSearch());
        searchDto.setResId(query.getResId());
        searchDto.setDirPrefix(query.getResDirPrefix());
        Optional.ofNullable(query.getResDirPrefix()).filter(StringUtils::isNotBlank)
                .ifPresent(l -> {
                    searchDto.setDirPrefix(l);
                    searchDto.setResId(null);
                });
        Optional.ofNullable(query.getResSize()).filter(StringUtils::isNotBlank)
                .map(CommonSearchDto.Field::of).ifPresent(searchDto::setResSize);
        Optional.ofNullable(query.getResType()).filter(l -> !CollectionUtils.isEmpty(l))
                .ifPresent(searchDto::setResTypeList);
        Optional.ofNullable(query.getModifiedAt()).filter(StringUtils::isNotBlank)
                .map(CommonSearchDto.Field::of).ifPresent(searchDto::setModifiedAt);
        Optional.of(CommonSearchDto.Paging.of(query.getLimit(), query.getPage()))
                .ifPresent(searchDto::setPaging);
        Optional.ofNullable(query.getOrder()).map(l -> CommonSearchDto.OrderBy.of(l.getField(), l.isAscOrder()))
                .ifPresent(searchDto::setOrder);
        SearchResultDto result = fullTextSearchRepo.commonSearch(searchDto);
        OPDSResultShowDto showDto = new OPDSResultShowDto();
        showDto.setSize(Math.toIntExact(result.getSize()));
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
            dto.setSearchableText(t.getContent());
            dto.setResDir(t.getResDir());
            dto.setContentType(contentType);
            return dto;
        }).filter(Objects::nonNull).collect(Collectors.toList()));
        return showDto;
    }

    @Override
    public ResourcePageDto searchByResId(String resId, String search) {
        ResourceHighlightHitsDto dto = fullTextSearchRepo.searchByResId(resId, search);
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(Objects.isNull(dto), "资源不存在");
        String highlightHtml = HTMLTemplateHelper.render("highlight.html",
                Collections.singletonMap("highlight", dto.getHighlights()));
        var t = dto.getResource();
        ResourcePageDto page = new ResourcePageDto();
        page.setResId(t.getResId());
        page.setResTitle(t.getResTitle());
        page.calculateSearchableResTitle();
        page.setResSize(FileHelper.formatFileSize(t.getResSize()));
        page.setModifiedAt(DateTimeHelper.tsToDt(t.getModifiedAt()));
        page.setRelativeResPath(t.calculateRelativePath(configProperties.getApp().indexDirectories()));
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
        page.setDirViewUrl(PageViewExtEnum.dirViewUrl(t.getResId(), t.calculateRelativeDir(configProperties.getApp().indexDirectories())));
        if (CollectionUtils.isEmpty(dto.getHighlights())) {
            page.setResTextSnippet(genResTextSnippet(t.getContent()));
        }
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
    public ResourceDownloadDto getDownloadResource(String resId) {
        ResourceDownloadDto dto = new ResourceDownloadDto();
        FullTextSearchModel res = fullTextSearchRepo.findByResId(resId);
        dto.setResId(resId);
        dto.setResTitle(res.getResTitle());
        dto.setAbsolutePath(res.calculateAbsolutePath());
        dto.setResDir(res.getResDir());
        return dto;
    }

    @Override
    public String getResourceContent(String resId) {
        FullTextSearchModel res = fullTextSearchRepo.findByResId(resId);
        return res.getContent();
    }
}
