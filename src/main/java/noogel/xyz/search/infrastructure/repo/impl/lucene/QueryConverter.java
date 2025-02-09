package noogel.xyz.search.infrastructure.repo.impl.lucene;

import noogel.xyz.search.infrastructure.dto.SearchQueryDto;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;

public class QueryConverter {
//
//    public static BooleanQuery.Builder genQueryBuilder(SearchQueryDto queryDto) {
//        BooleanQuery.Builder builder = new BooleanQuery.Builder();
//
//        if (!StringUtils.isEmpty(queryDto.getSearch())) {
//            String search = queryDto.getSearch();
//
//            // 模糊匹配 searchableText
//            QueryParser parser = new QueryParser("content", new IKAnalyzer());
//            parser.setFuzzyMinLength(1);
//            parser.setFuzzyMaxExpansions(10);
//
//            try {
//                Query searchableText = parser.parse(search + "~");
//                builder.add(searchableText, BooleanClause.Occur.SHOULD);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            // 短语匹配 searchableText
//            PhraseQuery.Builder searchableTextPhraseBuilder = new PhraseQuery.Builder();
//            String[] terms = search.split("\\s+");
//            for (int i = 0; i < terms.length; i++) {
//                searchableTextPhraseBuilder.add(new Term("content", terms[i]));
//            }
//            searchableTextPhraseBuilder.setSlop(50);
//            PhraseQuery searchableTextPhrase = searchableTextPhraseBuilder.build();
//            builder.add(searchableTextPhrase, BooleanClause.Occur.SHOULD);
//
//            // 模糊匹配 resName
//            QueryParser resNameParser = new QueryParser("resName", new IKAnalyzer());
//            resNameParser.setFuzzyMinLength(1);
//            resNameParser.setFuzzyMaxExpansions(10);
//            try {
//                Query resName = resNameParser.parse(search + "~");
//                builder.add(resName, BooleanClause.Occur.SHOULD);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            // 短语匹配 resName
//            PhraseQuery.Builder resNamePhraseBuilder = new PhraseQuery.Builder();
//            for (int i = 0; i < terms.length; i++) {
//                resNamePhraseBuilder.add(new Term("resName", terms[i]));
//            }
//            resNamePhraseBuilder.setSlop(10);
//            PhraseQuery resNamePhrase = resNamePhraseBuilder.build();
//            builder.add(resNamePhrase, BooleanClause.Occur.SHOULD);
//        }
//
//        if (!StringUtils.isEmpty(queryDto.getResType())) {
//            Term term = new Term("resType", queryDto.getResType());
//            TermQuery resType = new TermQuery(term);
//            builder.add(resType, BooleanClause.Occur.MUST);
//        }
//
//        if (!StringUtils.isEmpty(queryDto.getResDirPrefix())) {
//            Term term = new Term("resDir", queryDto.getResDirPrefix());
//            TermQuery resDir = new TermQuery(term);
//            builder.add(resDir, BooleanClause.Occur.MUST);
//        }
//
//        if (!StringUtils.isEmpty(queryDto.getResSize())) {
//            String[] sizeRange = queryDto.getResSize().split("-");
//            double lowerBound = Double.parseDouble(sizeRange[0]);
//            double upperBound = Double.parseDouble(sizeRange[1]);
//            RangeQuery rangeQuery = RangeQuery.newDoubleRange("resSize", lowerBound, upperBound, true, true);
//            builder.add(rangeQuery, BooleanClause.Occur.MUST);
//        }
//
//        if (!StringUtils.isEmpty(queryDto.getModifiedAt())) {
//            long currentTime = System.currentTimeMillis();
//            long modifiedTime = Long.parseLong(queryDto.getModifiedAt());
//            long timeDiff = currentTime - modifiedTime;
//
//            RangeQuery rangeQuery = RangeQuery.newLongRange("modifiedAt", modifiedTime, currentTime, true, true);
//            builder.add(rangeQuery, BooleanClause.Occur.MUST);
//        }
//
//        return builder;
//    }
}