package noogel.xyz.search.application.controller;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.consts.BaseConsts;
import noogel.xyz.search.infrastructure.dto.*;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.OPDSHelper;
import noogel.xyz.search.infrastructure.utils.UrlHelper;
import noogel.xyz.search.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/opds")
@Slf4j
public class OPDSCtrl {

    @Resource
    private SearchService searchService;
    @Resource
    private ConfigProperties configProperties;

    private static UrlDto collectUrls(HttpServletRequest httpServletRequest) {
        String requestUrl = httpServletRequest.getRequestURL().toString();
        String baseUrl = requestUrl.substring(0, requestUrl.indexOf(httpServletRequest.getRequestURI()));
        String searchUrl = baseUrl + "/opds?type=nav&text={searchTerms}&page=1";
        UrlDto dto = new UrlDto();
        dto.setRequestUrl(requestUrl);
        dto.setBaseUrl(baseUrl);
        dto.setSearchUrl(searchUrl);
        dto.setParameters(httpServletRequest.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, t -> t.getValue()[0])));
        return dto;
    }

    private void checkConfig() {
        String opdsDirectory = configProperties.getApp().getOpdsDirectory();
        ExceptionCode.CONFIG_ERROR.throwOn(StringUtils.isBlank(opdsDirectory), "OPDS 未开启");
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody
    String feedRoot(HttpServletRequest httpServletRequest,
                    @RequestParam(required = false, defaultValue = "") String type,
                    @RequestParam(required = false, defaultValue = "") String text,
                    @RequestParam(required = false, defaultValue = "1") int page) throws FeedException {
        checkConfig();
        UrlDto urlDto = collectUrls(httpServletRequest);
        SyndFeed syndFeed;
        if (BaseConsts.OPDS_TYPE_NAV.equals(type)) {
            syndFeed = entries(urlDto, text, page);
        } else {
            syndFeed = directories(urlDto);
        }
        return syndFeed == null ? "" : new SyndFeedOutput().outputString(syndFeed);
    }

    @RequestMapping(value = "/res", method = RequestMethod.GET)
    public void resource(HttpServletRequest httpServletRequest,
                         HttpServletResponse httpServletResponse,
                         @RequestParam(required = false, defaultValue = "") String type,
                         @RequestParam(required = false, defaultValue = "") String resId) throws FeedException {
        checkConfig();
        ResourceDownloadDto downloadResource = searchService.getDownloadResource(resId);
        String resPath = "";
        String resTitle = "unknown";
        switch (type) {
            case BaseConsts.OPDS_TYPE_FILE:
                resPath = downloadResource.getAbsolutePath();
                resTitle = downloadResource.getResTitle();
                break;
            case BaseConsts.OPDS_TYPE_COVER:
                OPDSResMetaDataDto opdsResMetaDataDto = OPDSHelper.readMetaData(downloadResource.getResDir());
                ExceptionCode.FILE_ACCESS_ERROR.throwOn(Objects.isNull(opdsResMetaDataDto), "资源不存在");
                resPath = opdsResMetaDataDto.getAbsoluteDir() + "/" + opdsResMetaDataDto.getCover();
                resTitle = opdsResMetaDataDto.getCover();
                break;
            default:
                throw ExceptionCode.FILE_ACCESS_ERROR.throwExc("资源类型错误");
        }
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(StringUtils.isBlank(resPath), "资源不存在");
        download(resPath, resTitle, httpServletResponse);

    }

    /**
     * 目录
     *
     * @param urlDto
     * @return
     */
    public SyndFeed directories(UrlDto urlDto) {
        List<SyndLink> linkList = OPDSHelper.ofRootSyndLink(urlDto);
        SyndEntry entryNav = OPDSHelper.ofSyndEntry("畅文全索", "/opds?type=nav&page=1", "每次若干条资源");
        return OPDSHelper.index(urlDto.getBaseUrl(), linkList, List.of(entryNav));
    }

    /**
     * 资源
     *
     * @param urlDto
     * @return
     */
    public SyndFeed entries(UrlDto urlDto, String text, int page) {
        SearchQueryDto query = new SearchQueryDto();
        query.setLimit(BaseConsts.DEFAULT_LIMIT);
        query.setPage(page);
        query.setSearch(text);
        query.setResDirPrefix(configProperties.getApp().getOpdsDirectory());
        query.setOrder(SearchQueryDto.buildLatestOrder(false));
        OPDSResultShowDto result = searchService.opdsSearch(query);

        // link
        List<SyndLink> linkList = OPDSHelper.ofRootSyndLink(urlDto);

        // data
        List<SyndEntry> entries = result.getData().stream().map(t -> {
            // metadata
            List<SyndLink> links = new ArrayList<>();
            SyndLink syndLinkAcqui = new SyndLinkImpl();
            syndLinkAcqui.setType(t.getContentType());
            syndLinkAcqui.setHref("/opds/res?type=file&resId=" + t.getResId());
            syndLinkAcqui.setRel(BaseConsts.OPDS_LINK_ACQUISITION);
            syndLinkAcqui.setTitle("download");
            syndLinkAcqui.setLength(t.getResSize());
            links.add(syndLinkAcqui);

            OPDSResMetaDataDto metaData = OPDSHelper.readMetaData(t.getResDir());
            if (Objects.nonNull(metaData)) {
                String coverDir = metaData.getAbsoluteDir() + metaData.getCover();
                File file = new File(coverDir);
                String coverContentType = FileHelper.getContentType(file);
                SyndLink syndLinkCover = new SyndLinkImpl();
                syndLinkCover.setType(coverContentType);
                syndLinkCover.setHref("/opds/res?type=cover&resId=" + t.getResId());
                syndLinkCover.setRel(BaseConsts.OPDS_LINK_COVER);
                links.add(syndLinkCover);
                SyndLink syndLinkThum = new SyndLinkImpl();
                syndLinkThum.setType(coverContentType);
                syndLinkThum.setHref("/opds/res?type=cover&resId=" + t.getResId());
                syndLinkThum.setRel(BaseConsts.OPDS_LINK_THUMBNAIL);
                links.add(syndLinkThum);
                SyndLink syndLinkImage = new SyndLinkImpl();
                syndLinkImage.setType(coverContentType);
                syndLinkImage.setHref("/opds/res?type=cover&resId=" + t.getResId());
                syndLinkImage.setRel(BaseConsts.OPDS_LINK_IMAGE);
                links.add(syndLinkImage);
                SyndLink syndLinkImageThum = new SyndLinkImpl();
                syndLinkImageThum.setType(coverContentType);
                syndLinkImageThum.setHref("/opds/res?type=cover&resId=" + t.getResId());
                syndLinkImageThum.setRel(BaseConsts.OPDS_LINK_IMAGE_THUMBNAIL);
                links.add(syndLinkImageThum);
            }

            SyndEntry syndEntry = new SyndEntryImpl();
            syndEntry.setTitle(t.getResTitle());
            syndEntry.setAuthor(Optional.ofNullable(metaData)
                    .map(l -> String.join("；", l.getCreator())).orElse("未知"));
            syndEntry.setUpdatedDate(Date.from(Instant.ofEpochMilli(t.getModifiedAt())));
            syndEntry.setPublishedDate(Date.from(Instant.ofEpochMilli(t.getModifiedAt())));
            syndEntry.setLinks(links);
            Optional.ofNullable(metaData).map(OPDSResMetaDataDto::getDescription)
                    .filter(StringUtils::isNotBlank).ifPresent(l -> {
                        String lContent = l.replace("\n", " ");
                        if (l.length() > 100) {
                            lContent = lContent.substring(0, 100) + "...";
                        }
                        SyndContent syndContent = new SyndContentImpl();
                        syndContent.setValue(lContent);
                        syndEntry.setContents(Collections.singletonList(syndContent));
                    });
            return syndEntry;
        }).collect(Collectors.toList());

        List<SyndLink> pageSyndLink = OPDSHelper.ofPageSyndLink(urlDto, page, result.getSize());
        linkList.addAll(pageSyndLink);

        return OPDSHelper.index(urlDto.getBaseUrl(), linkList, entries);
    }

    public void download(String resPath, String title, HttpServletResponse response) {
        File file = new File(resPath);
        String contentType = "application/octet-stream";
        String probeContentType = FileHelper.getContentType(file);
        if (StringUtils.isNotBlank(probeContentType)) {
            contentType = probeContentType;
        }

        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        try (InputStream inputStream = new FileInputStream(file)) {
            response.reset();
            response.setContentType(contentType);
            response.addHeader("Content-Length", String.valueOf(file.length()));
            response.addHeader("Content-Disposition", "attachment; filename=\""
                    + UrlHelper.ct(title.replaceAll("[;='\"]", "_"))
                    + "\"; filename*=utf-8''"
                    + UrlHelper.ct(title));

            try (ServletOutputStream outputStream = response.getOutputStream()) {
                byte[] b = new byte[1024];
                int len;
                while ((len = inputStream.read(b)) > 0) {
                    outputStream.write(b, 0, len);
                }
            }
        } catch (Exception ex) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(ex);
        }
    }

}
