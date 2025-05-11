package noogel.xyz.search.infrastructure.repo.impl.elastic;

import java.util.HashMap;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.mapping.IntegerNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.LongNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TermVectorOption;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;

public class ElasticMapping {

    public static Map<String, Property> generate() {
        Map<String, Property> documentMap = new HashMap<>();
        documentMap.put("resId", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("resDir", Property
                .of(p -> p.text(TextProperty.of(i -> i
                    .index(true)
                    .analyzer("path_tokenizer")))));
        documentMap.put("resName", Property
                .of(p -> p.text(TextProperty.of(i -> i
                    .index(true)
                    .analyzer("ik_max_word")
                    .searchAnalyzer("ik_smart")
                    .termVector(TermVectorOption.WithPositionsOffsets)))));
        documentMap.put("resTitle", Property
                .of(p -> p.text(TextProperty.of(i -> i
                    .index(true)
                    .analyzer("ik_max_word") 
                    .searchAnalyzer("ik_smart")
                    .termVector(TermVectorOption.WithPositionsOffsets)))));
        documentMap.put("resHash", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("resType", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("resSize", Property
                .of(p -> p.long_(LongNumberProperty.of(i -> i.index(true)))));
        documentMap.put("modifiedAt", Property
                .of(p -> p.long_(LongNumberProperty.of(i -> i.index(true)))));
        documentMap.put("resRank", Property
                .of(p -> p.long_(LongNumberProperty.of(i -> i.index(true)))));
        documentMap.put("content", Property
                .of(p -> p.text(TextProperty.of(i -> i
                    .index(true)
                    .analyzer("ik_smart")
                    .termVector(TermVectorOption.WithPositionsOffsets)))));
        documentMap.put("contentHash", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("contentSize", Property
                .of(p -> p.integer(IntegerNumberProperty.of(i -> i.index(true)))));
        return documentMap;
    }

}
