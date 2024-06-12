package noogel.xyz.search.application.controller;

import jakarta.annotation.Resource;
import noogel.xyz.search.infrastructure.dto.SearchBaseQueryDto;
import noogel.xyz.search.infrastructure.dto.SearchQueryDto;
import noogel.xyz.search.infrastructure.dto.page.ResourcePageDto;
import noogel.xyz.search.infrastructure.dto.page.SearchResultShowDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class SearchCtrl {

    @Resource
    private SearchService searchService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView search(@RequestParam(required = false) String search,
                               @RequestParam(required = false) String relativeResDir,
                               @RequestParam(required = false) String resId,
                               @RequestParam(required = false) String resSize,
                               @RequestParam(required = false) String resType,
                               @RequestParam(required = false) String modifiedAt,
                               @RequestParam(required = false) Boolean random,
                               @RequestParam(required = false, defaultValue = "20") int limit,
                               @RequestParam(required = false, defaultValue = "0") int offset) {
        ModelAndView mv = new ModelAndView("index");
        SearchQueryDto query = new SearchQueryDto();
        // text search
        query.setSearch(search);
        query.setResSize(resSize);
        query.setModifiedAt(modifiedAt);
        query.setResType(resType);
        // common
        query.setLimit(limit);
        query.setOffset(offset);
        // path search
        if (StringUtils.isNotBlank(relativeResDir)) {
            ExceptionCode.PARAM_ERROR.throwOn(StringUtils.isBlank(resId), "资源 ID 不存在");
            String absPath = searchService.getDownloadResource(resId).getAbsolutePath();
            String resDirPrefix = absPath.substring(0, absPath.indexOf(relativeResDir) + relativeResDir.length());
            // 设置参数
            query.setResDirPrefix(resDirPrefix);
            query.setRelativeResDir(relativeResDir);
            query.setResId(resId);
        }
        // 排序规则
        // 随机标记最高优
        // 主页默认最近更新
        // 主页搜索自动排序
        // 目录页默认 rank
        // 目录页搜索自动排序
        if (Boolean.TRUE.equals(random)) {
            // 随机标记最高优
            query.setRandomScore(true);
        } else if (SearchQueryDto.indexEmptySearch(query)) {
            // 主页设置最近更新
            query.setOrder(SearchBaseQueryDto.buildLatestOrder(false));
        } else if (SearchQueryDto.dirEmptySearch(query)) {
            // 目录页默认 rank
            query.setOrder(SearchBaseQueryDto.buildRankOrder(true));
        }
        SearchResultShowDto result = searchService.pageSearch(query);
        mv.addObject("result", result);
        mv.addObject("random", String.valueOf(random));
        return mv;
    }

    @RequestMapping(value = "/search/{resId}", method = RequestMethod.GET)
    public @ResponseBody
    ResourcePageDto searchByResId(@PathVariable String resId, @RequestParam(required = false) String search) {
        return searchService.searchByResId(resId, search);
    }

}
