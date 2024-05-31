package noogel.xyz.search.infrastructure.utils;

import java.util.List;
import java.util.function.Function;

public class StreamHelper {

    /**
     * 在 rList 中存在，在 lList 中不存在的返回
     *
     * @param lList
     * @param lMapper
     * @param rList
     * @param rMapper
     * @param <L>
     * @param <R>
     * @return
     */
    public static <L, R> List<R> left(List<L> lList, Function<? super L, String> lMapper,
                                      List<R> rList, Function<? super R, String> rMapper) {
        List<String> collected = lList.stream().map(lMapper).toList();
        return rList.stream().filter(t -> {
            String apply = rMapper.apply(t);
            return !collected.contains(apply);
        }).toList();
    }
}
