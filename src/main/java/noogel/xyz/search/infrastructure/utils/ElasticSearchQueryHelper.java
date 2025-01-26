package noogel.xyz.search.infrastructure.utils;

import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import javax.annotation.Nullable;
import java.util.function.Function;

public class ElasticSearchQueryHelper {

    @Nullable
    public static Query buildRangeQuery(String field, String val, Function<String, Double> fn) {
        String[] split = val.split(":");
        String cmp = split[0];
        String vue = split[1];
        if ("gt".equalsIgnoreCase(cmp)) {
            return NumberRangeQuery.of(t -> t.field(field).gt(fn.apply(vue)))._toRangeQuery()._toQuery();
        } else if ("lt".equalsIgnoreCase(cmp)) {
            return NumberRangeQuery.of(t -> t.field(field).lt(fn.apply(vue)))._toRangeQuery()._toQuery();
        }
        return null;
    }
}
