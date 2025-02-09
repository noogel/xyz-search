package noogel.xyz.search.application.controller;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.lucene.LuceneAnalyzer;
import noogel.xyz.search.infrastructure.lucene.LuceneSearcher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Slf4j
public class TestCtrl {

    private LuceneSearcher luceneSearcher;

    @Resource
    private ConfigProperties configProperties;

    @PostConstruct
    public void init() {
        luceneSearcher = new LuceneSearcher(configProperties.getBase().indexerFilePath(), LuceneAnalyzer.ANALYZER_WRAPPER);
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public @ResponseBody String test() {
        luceneSearcher.search("健康");
        return "{}";
    }
}
