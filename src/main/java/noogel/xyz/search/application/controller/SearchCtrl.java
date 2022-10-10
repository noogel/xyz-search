package noogel.xyz.search.application.controller;

import noogel.xyz.search.infrastructure.dto.ResourcePageDto;
import noogel.xyz.search.infrastructure.dto.SearchQueryDto;
import noogel.xyz.search.infrastructure.dto.SearchResultShowDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;

@Controller
public class SearchCtrl {

    @Resource
    private SearchService searchService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView search(@RequestParam(required = false) String search,
                               @RequestParam(required = false) String resSize,
                               @RequestParam(required = false) String modifiedAt,
                               @RequestParam(required = false, defaultValue = "20") int limit,
                               @RequestParam(required = false, defaultValue = "0") int offset) {
        ModelAndView mv = new ModelAndView("index");
        SearchQueryDto query = new SearchQueryDto();
        query.setSearch(search);
        query.setResSize(resSize);
        query.setModifiedAt(modifiedAt);
        query.setLimit(limit);
        query.setOffset(offset);
        SearchResultShowDto result = searchService.search(query);
        mv.addObject("result", result);
        return mv;
    }

}
