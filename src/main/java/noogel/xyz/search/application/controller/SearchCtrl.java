package noogel.xyz.search.application.controller;

import jakarta.annotation.Resource;
import noogel.xyz.search.infrastructure.dto.SearchQueryDto;
import noogel.xyz.search.infrastructure.dto.SearchResultShowDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
        SearchResultShowDto result = searchService.pageSearch(query);
        mv.addObject("result", result);
        return mv;
    }

}
