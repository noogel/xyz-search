package noogel.xyz.search.infrastructure.repo.impl.lucene;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.lucene.LuceneAnalyzer;
import noogel.xyz.search.infrastructure.lucene.LuceneSearcher;
import noogel.xyz.search.infrastructure.lucene.LuceneWriter;
import noogel.xyz.search.infrastructure.lucene.Paging;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;
import noogel.xyz.search.infrastructure.repo.FullTextSearchRepo;
import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto;
import noogel.xyz.search.infrastructure.dto.repo.RandomSearchDto;
import noogel.xyz.search.infrastructure.utils.ElasticSearchQueryHelper;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
public class LuceneFullTextSearchRepoImpl implements FullTextSearchRepo {
    @Resource
    private volatile ConfigProperties configProperties;

    private LuceneWriter luceneWriter;
    private LuceneSearcher luceneSearcher;


    @PostConstruct
    public void init() {
        luceneWriter = new LuceneWriter(configProperties.getBase().indexerFilePath(), LuceneAnalyzer.ANALYZER_WRAPPER);
        luceneSearcher = new LuceneSearcher(configProperties.getBase().indexerFilePath(), LuceneAnalyzer.ANALYZER_WRAPPER);
    }


    @Override
    public boolean delete(String resId) {
        FullTextSearchModel model = new FullTextSearchModel();
        model.setResId(resId);
        return luceneWriter.delete(model);
    }

    @Override
    public boolean upsert(FullTextSearchModel model) {
        log.info("full {}", JsonHelper.toJson(model));
        luceneWriter.write(model);
        return true;
    }

    @Override
    public void forceMerge() {
        luceneWriter.forceMerge();
    }

    @Override
    public void reset() {
        luceneWriter.reset();
    }

    @Override
    public FullTextSearchModel findByResId(String resId) {
        return null;
    }

    @Override
    public List<FullTextSearchModel> findByResHash(String resHash) {
        return List.of();
    }

    @Override
    public ResourceHighlightHitsDto searchByResId(String resId, @Nullable String text) {
        return null;
    }

    @Override
    public SearchResultDto commonSearch(CommonSearchDto searchDto) {
        try {
            Query query = genQueryBuilder(searchDto);
            Pair<TotalHits, List<Document>> totalHitsListPair = luceneSearcher.pagingSearch(query, Paging.of(1, 2));
            SearchResultDto resultDto = new SearchResultDto();
            resultDto.setData2(new ArrayList<>());
            resultDto.setSize(totalHitsListPair.getKey().value);
            resultDto.setExactSize(TotalHits.Relation.EQUAL_TO.equals(totalHitsListPair.getKey().relation));
            return resultDto;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchResultDto randomSearch(RandomSearchDto searchDto) {
        return null;
    }

    private static Query genQueryBuilder(CommonSearchDto searchDto) throws ParseException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        if (!StringUtils.isEmpty(searchDto.getSearchQuery())) {
            // 创建查询解析器，并指定要搜索的字段列表
            String[] fields = {"resName", "content"};
            Map<String, Float> boosts = Map.of("resName", 500.F, "content", 100.F);
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, LuceneAnalyzer.ANALYZER_MAP.get("content"), boosts);
            Query query = parser.parse(searchDto.getSearchQuery());
            builder.add(query, BooleanClause.Occur.MUST);
        }
        if (!CollectionUtils.isEmpty(searchDto.getResTypeList())) {
            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            for (String resType : searchDto.getResTypeList()) {
                Term term = new Term("resType", resType);
                TermQuery termQuery = new TermQuery(term);
                booleanQueryBuilder.add(termQuery, BooleanClause.Occur.SHOULD);
            }
            builder.add(booleanQueryBuilder.build(), BooleanClause.Occur.MUST);
        }
        if (!StringUtils.isEmpty(searchDto.getDirPrefix())) {
            Term term = new Term("resDir", searchDto.getDirPrefix());
            TermQuery resDir = new TermQuery(term);
            builder.add(resDir, BooleanClause.Occur.MUST);
        }
        if (Objects.nonNull(searchDto.getResSize())) {
            CommonSearchDto.Field resSize = searchDto.getResSize();
            int lowerPrice = Integer.parseInt(resSize.getValue());
            int upperPrice = Integer.parseInt(resSize.getValue());
            if (CommonSearchDto.CompareEnum.GT.equals(resSize.getCompare())) {
                upperPrice = Integer.MAX_VALUE;
            } else {
                lowerPrice = 0;
            }
            Query rangeQuery = IntPoint.newRangeQuery("resSize", lowerPrice, upperPrice);
            builder.add(rangeQuery, BooleanClause.Occur.MUST);
        }
        if (Objects.nonNull(searchDto.getModifiedAt())) {
            CommonSearchDto.Field modifiedAt = searchDto.getModifiedAt();
            int lowerPrice = Integer.parseInt(modifiedAt.getValue());
            int upperPrice = Integer.parseInt(modifiedAt.getValue());
            if (CommonSearchDto.CompareEnum.GT.equals(modifiedAt.getCompare())) {
                upperPrice = Integer.MAX_VALUE;
            } else {
                lowerPrice = 0;
            }
            Query rangeQuery = IntPoint.newRangeQuery("modifiedAt", lowerPrice, upperPrice);
            builder.add(rangeQuery, BooleanClause.Occur.MUST);
        }
        return builder.build();
    }

}
