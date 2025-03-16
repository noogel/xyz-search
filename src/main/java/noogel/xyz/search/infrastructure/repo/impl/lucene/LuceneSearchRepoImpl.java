package noogel.xyz.search.infrastructure.repo.impl.lucene;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.consts.CommonsConsts;
import noogel.xyz.search.infrastructure.dto.LLMSearchResultDto;
import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto;
import noogel.xyz.search.infrastructure.dto.repo.LLMSearchDto;
import noogel.xyz.search.infrastructure.dto.repo.RandomSearchDto;
import noogel.xyz.search.infrastructure.lucene.*;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;
import noogel.xyz.search.infrastructure.repo.FullTextSearchRepo;
import noogel.xyz.search.infrastructure.utils.pool.BatchProcessor;
import noogel.xyz.search.infrastructure.utils.pool.BatchProcessorFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static noogel.xyz.search.infrastructure.lucene.LuceneAnalyzer.*;

@Service
@Slf4j
public class LuceneSearchRepoImpl implements FullTextSearchRepo {
    @Resource
    private volatile ConfigProperties configProperties;

    @Resource
    private BatchProcessorFactory batchProcessorFactory;

    private LuceneWriter luceneWriter;
    private LuceneSearcher luceneSearcher;
    private BatchProcessor<FullTextSearchModel> batchUpsertProcessor;
    private BatchProcessor<String> deleteBatchProcessor;

    private static Query genQueryBuilder(CommonSearchDto searchDto) throws ParseException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        if (!StringUtils.isEmpty(searchDto.getSearchQuery())) {
            // 创建查询解析器，并指定要搜索的字段列表
            String[] fields = {"resName", "content"};
            Map<String, Float> boosts = Map.of("resName", 500.F, "content", 100.F);
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, new SmartChineseAnalyzer(STOPWORDS), boosts);
            String searchQuery = replace(searchDto.getSearchQuery());
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
                Term term = new Term("resType", replace(resType));
                TermQuery termQuery = new TermQuery(term);
                booleanQueryBuilder.add(termQuery, BooleanClause.Occur.SHOULD);
            }
            builder.add(booleanQueryBuilder.build(), BooleanClause.Occur.MUST);
        }
        if (!StringUtils.isEmpty(searchDto.getDirPrefix())) {
            Term term = new Term("resDir", replace(searchDto.getDirPrefix()));
            Query resDir = new PrefixQuery(term);
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
            Query rangeQuery = LongPoint.newRangeQuery("resSize", lowerPrice, upperPrice);
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
            Query rangeQuery = LongPoint.newRangeQuery("modifiedAt", lowerPrice, upperPrice);
            builder.add(rangeQuery, BooleanClause.Occur.MUST);
        }
        return builder.build();
    }

    private static String replace(String sq) {
        return sq.replace("+", "\\+")
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
    }

    private static SortField.Type convertSortType(String field) {
        try {
            Field declaredField = FullTextSearchModel.class.getDeclaredField(field);
            Class<?> declaringClass = declaredField.getType();
            if (String.class.equals(declaringClass)) {
                return SortField.Type.STRING;
            } else if (Integer.class.equals(declaringClass)) {
                return SortField.Type.INT;
            } else if (Long.class.equals(declaringClass)) {
                return SortField.Type.LONG;
            } else {
                throw new RuntimeException("未知的类型");
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void init() {
        luceneWriter = new LuceneWriter(configProperties.getBase().indexerFilePath(),
                new PerFieldAnalyzerWrapper(DEFAULT_ANALYZER, generateAnalyzer()));
        luceneSearcher = new LuceneSearcher(configProperties.getBase().indexerFilePath(),
                new PerFieldAnalyzerWrapper(DEFAULT_ANALYZER, generateAnalyzer()));

        // 初始化批处理器，每100条数据或每5秒处理一次
        batchUpsertProcessor = batchProcessorFactory.getOrCreate(
                "lucene-writer",
                100,
                CommonsConsts.DEFAULT_BATCH_COMMIT_LIMIT_MS,
                this::batchUpsert
        );

        // 初始化删除批处理器
        deleteBatchProcessor = batchProcessorFactory.getOrCreate(
                "lucene-deleter",
                50,
                CommonsConsts.DEFAULT_BATCH_COMMIT_LIMIT_MS,
                this::batchDelete
        );
    }

    @PreDestroy
    public void destroy() {
        batchUpsertProcessor.shutdown();
        deleteBatchProcessor.shutdown();
        luceneSearcher.close();
        luceneWriter.close();
    }

    /**
     * 批量处理数据
     */
    private void batchUpsert(List<FullTextSearchModel> models) {
        log.info("批量添加 {} 条数据：\n{}", models.size(), models.stream()
                .map(FullTextSearchModel::calculateAbsolutePath).collect(Collectors.joining("\n")));
        for (FullTextSearchModel model : models) {
            luceneWriter.update(model);
        }
        // 批量处理完成后提交更改
        luceneWriter.commit();
    }

    /**
     * 批量删除数据
     */
    private void batchDelete(List<String> resIds) {
        log.info("批量删除 {} 条数据", resIds.size());
        for (String resId : resIds) {
            FullTextSearchModel model = new FullTextSearchModel();
            model.setResId(resId);
            luceneWriter.delete(model);
        }
        // 批量删除完成后提交更改
        luceneWriter.commit();
    }

    @Override
    public boolean delete(String resId, Runnable onSuccess) {
        // 添加到删除批处理队列，带回调
        return deleteBatchProcessor.add(resId, onSuccess);
    }

    @Override
    public boolean upsert(FullTextSearchModel model, Runnable onSuccess) {
        // 添加到批处理队列
        return batchUpsertProcessor.add(model, onSuccess);
    }

    @Override
    public void forceMerge() {
        luceneWriter.forceMerge();
        luceneWriter.commit();
    }

    @Override
    public void reset() {
        luceneWriter.reset();
        luceneWriter.commit();
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
            HighlightOptions highlightOptions = HighlightOptions.of(
                    100, 10, "<em>", "</em>");
            Pair<LuceneDocument, List<String>> resp = luceneSearcher.findFirstWithHighlight(query, highlightOptions);
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

            // 分页
            Paging paging;
            if (Objects.nonNull(searchDto.getPaging())) {
                Integer limit = searchDto.getPaging().getLimit();
                Integer page = searchDto.getPaging().getPage();
                paging = Paging.of(Math.max(page, 1), limit);
            } else {
                paging = Paging.of(1, 20);
            }

            // 排序
            OrderBy order = null;
            if (Objects.nonNull(searchDto.getOrder())) {
                order = OrderBy.of(searchDto.getOrder().getField(),
                        convertSortType(searchDto.getOrder().getField()), searchDto.getOrder().isAsc());
            }

            Pair<Integer, List<LuceneDocument>> totalHitsListPair = luceneSearcher.pagingSearch(query, paging, order);
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

    @Override
    public LLMSearchResultDto llmSearch(LLMSearchDto searchDto) {
        try {
            CommonSearchDto commonSearchDto = new CommonSearchDto();
            commonSearchDto.setSearchQuery(searchDto.getSearchQuery());
            commonSearchDto.setDirPrefix(searchDto.getDirPrefix());
            Query query = genQueryBuilder(commonSearchDto);
            if (StringUtils.isBlank(query.toString())) {
                query = new WildcardQuery(new Term("content", "*"));
            }
            Paging paging = Paging.of(1, searchDto.getMaxNumFragments());
            HighlightOptions highlightOptions = HighlightOptions.of(
                    searchDto.getFragmentSize(), searchDto.getMaxNumFragments(), "", "");
            var result = luceneSearcher.llmSearch(query, paging, highlightOptions);
            LLMSearchResultDto resultDto = new LLMSearchResultDto();
            resultDto.setDocuments(result.getKey().stream()
                    .map(l -> (FullTextSearchModel) l).collect(Collectors.toList()));
            resultDto.setHighlights(result.getValue());
            return resultDto;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
