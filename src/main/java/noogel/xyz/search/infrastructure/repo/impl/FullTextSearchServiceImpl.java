package noogel.xyz.search.infrastructure.repo.impl;

import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.repo.FullTextSearchRepo;
import noogel.xyz.search.infrastructure.repo.FullTextSearchService;
import noogel.xyz.search.infrastructure.repo.impl.elastic.ElasticSearchRepoImpl;
import noogel.xyz.search.infrastructure.repo.impl.lucene.LuceneSearchRepoImpl;

@Service
public class FullTextSearchServiceImpl implements FullTextSearchService {

    @Resource
    private LuceneSearchRepoImpl luceneSearchRepo;
    @Resource
    private ElasticSearchRepoImpl elasticSearchRepo;
    @Resource
    private ConfigProperties configProperties;

    @Override
    public FullTextSearchRepo getBean() {
        if (configProperties.getApp().getChat().getElastic().isEnable()) {
            return elasticSearchRepo;
        }
        return luceneSearchRepo;
    }
}
