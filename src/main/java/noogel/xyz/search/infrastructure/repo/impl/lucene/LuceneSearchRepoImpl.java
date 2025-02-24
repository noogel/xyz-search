package noogel.xyz.search.infrastructure.repo.impl.lucene;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto;
import noogel.xyz.search.infrastructure.dto.repo.RandomSearchDto;
import noogel.xyz.search.infrastructure.lucene.*;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;
import noogel.xyz.search.infrastructure.repo.FullTextSearchRepo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class LuceneSearchRepoImpl implements FullTextSearchRepo {
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
        return luceneWriter.write(model);
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
        try {
            CommonSearchDto searchDto = new CommonSearchDto();
            searchDto.setResId(resId);
            Query query = genQueryBuilder(searchDto);
            return (FullTextSearchModel) luceneSearcher.findFirst(query);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResourceHighlightHitsDto searchByResId(String resId, @Nullable String searchQuery) {
        try {
            CommonSearchDto searchDto = new CommonSearchDto();
            searchDto.setSearchQuery(searchQuery);
            searchDto.setResId(resId);
            Query query = genQueryBuilder(searchDto);
            Pair<LuceneDocument, List<String>> resp = luceneSearcher.findFirstWithHighlight(query);
            ResourceHighlightHitsDto dto = new ResourceHighlightHitsDto();
            dto.setResource((FullTextSearchModel) resp.getLeft());
            dto.setHighlights(resp.getRight());
            return dto;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchResultDto commonSearch(CommonSearchDto searchDto) {
        try {
            Query query = genQueryBuilder(searchDto);
            if (StringUtils.isBlank(query.toString())) {
                query = new WildcardQuery(new Term("content", "*"));
            }

            Paging paging;
            if (Objects.nonNull(searchDto.getPaging())) {
                Integer limit = searchDto.getPaging().getLimit();
                Integer offset = searchDto.getPaging().getOffset();
                paging = Paging.of(offset / limit + 1, limit);
            } else {
                paging = Paging.of(1, 20);
            }

            Pair<Integer, List<LuceneDocument>> totalHitsListPair = luceneSearcher.pagingSearch(query, paging);
            SearchResultDto resultDto = new SearchResultDto();
            resultDto.setData(totalHitsListPair.getValue().stream().map(t -> (FullTextSearchModel) t).toList());
            resultDto.setSize(totalHitsListPair.getKey());
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
            String searchQuery = searchDto.getSearchQuery()
                    .replace("+", "\\+")
                    .replace("-", "\\-")
                    .replace("&", "\\&")
                    .replace("|", "\\|")
                    .replace("!", "\\!")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("{", "\\{")
                    .replace("}", "\\}")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace("^", "\\^")
                    .replace("\"", "\\\"")
                    .replace("~", "\\~")
                    .replace("*", "\\*")
                    .replace("?", "\\?")
                    .replace(":", "\\:")
                    .replace("\\", "\\\\");
            Query query = parser.parse(searchQuery);
            builder.add(query, BooleanClause.Occur.MUST);
        }
        if (StringUtils.isNotBlank(searchDto.getResId())) {
            Term term = new Term("resId", searchDto.getResId());
            TermQuery resDir = new TermQuery(term);
            builder.add(resDir, BooleanClause.Occur.MUST);
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
