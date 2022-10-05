package noogel.xyz.search.infrastructure.utils;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.function.Function;

public class ElasticSearchQueryHelper {

    @Nullable
    public static <T> Query buildRangeQuery(String field, String val, Function<String, T> fn) {
        String[] split = val.split(":");
        String cmp = split[0];
        String vue = split[1];
        if ("gt".equalsIgnoreCase(cmp)) {
            return RangeQuery.of(t-> t.field(field).gt(JsonData.of(fn.apply(vue))))._toQuery();
        } else if ("lt".equalsIgnoreCase(cmp)) {
            return RangeQuery.of(t-> t.field(field).lt(JsonData.of(fn.apply(vue))))._toQuery();
        }
        return null;
    }
}
