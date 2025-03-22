package noogel.xyz.search.application.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.SearchQueryDto;
import noogel.xyz.search.infrastructure.dto.page.SearchResultShowDto;
import noogel.xyz.search.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/open-api")
@Slf4j
public class OpenApiCtrl {
    @Resource
    private SearchService searchService;

    /**
     * q=全面的健康管理方法&format=json&pageno=1&safesearch=1&language=en-US&time_range=&categories=&theme=simple&image_proxy=0
     *
     * @param q
     * @param limit
     * @return
     */
    @RequestMapping(value = "/searxng/search", method = RequestMethod.GET)
    public ModelAndView search(HttpServletRequest request, HttpServletResponse response,
                               @RequestHeader MultiValueMap<String, String> headers,
                               @RequestParam String q,
                               @RequestParam(required = false, defaultValue = "json") String format,
                               @RequestParam(required = false, defaultValue = "1") int pageno,
                               @RequestParam(required = false, defaultValue = "1") int limit) {
        ModelAndView mv = new ModelAndView("searxng");
        SearchQueryDto queryDto = new SearchQueryDto();
        queryDto.setSearch(q);
        queryDto.setLimit(limit);
        queryDto.setPage(pageno);
        SearchResultShowDto result = searchService.pageSearch(queryDto);
        mv.addObject("result", result);
        return mv;
    }

}
