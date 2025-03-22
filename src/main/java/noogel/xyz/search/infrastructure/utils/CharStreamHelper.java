package noogel.xyz.search.infrastructure.utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CharStreamHelper {
    // URL 格式验证
    private static final String URL_REGEX = "^((http|https)://)?([\\w-]+\\.)+[\\w-]+(/[\\w-?=&/%.#]*)?$";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
    private static final String ENG_REGEX = "^[A-Za-z',.!?]*$";
    private static final Pattern ENG_PATTERN = Pattern.compile(ENG_REGEX);
    private static final String DIG_REGEX = "^[0-9]*$";
    private static final Pattern DIG_PATTERN = Pattern.compile(DIG_REGEX);
    private static final String TWO_HAN_REGEX = "^[\\u4e00-\\u9fa5]{2}$";
    private static final Pattern TWO_HAN_PATTERN = Pattern.compile(TWO_HAN_REGEX);
    // 预编译正则提升性能
    private static final Pattern CH_EN_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5])([A-Za-z0-9])|([A-Za-z0-9])([\\u4e00-\\u9fa5])"
    );

    private static String insertSpaceBetweenChEn(String text) {
        return CH_EN_PATTERN.matcher(text)
                .replaceAll(mr ->
                        mr.group(1) != null ?
                                mr.group(1) + " " + mr.group(2) :
                                mr.group(3) + " " + mr.group(4)
                );
    }

    public static String opt(String pageContent) {
        List<String> result = Arrays.stream(pageContent.split("\n")).toList();
        StringBuilder stringBuilder = new StringBuilder();
        result.forEach(item -> {
            String current = item.replace(" ", "");
            if (URL_PATTERN.matcher(current).find()) {
                stringBuilder.append(" ");
                stringBuilder.append(insertSpaceBetweenChEn(current));
                stringBuilder.append(" ");
            } else if (ENG_PATTERN.matcher(current).find()) {
                stringBuilder.append(" ");
                if (item.length() / (float) current.length() < 1.5) {
                    stringBuilder.append(item);
                } else {
                    stringBuilder.append(current);
                }
                stringBuilder.append(" ");
            } else if (DIG_PATTERN.matcher(current).find()) {
                stringBuilder.append(" ");
                stringBuilder.append(current);
                stringBuilder.append(" ");
            } else if (TWO_HAN_PATTERN.matcher(current).find()) {
                stringBuilder.append(" ");
                stringBuilder.append(current);
                stringBuilder.append(" ");
            } else {
                if (Stream.of("------", "======").anyMatch(current::startsWith)) {
                    stringBuilder.append(" ");
                }
                if (Stream.of("1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.",
                        "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "•").anyMatch(current::startsWith)) {
                    stringBuilder.append(" ");
                }
                stringBuilder.append(insertSpaceBetweenChEn(current));
                if (Stream.of("------", "======").anyMatch(current::endsWith)) {
                    stringBuilder.append(" ");
                }
                if (Stream.of(".", "。").anyMatch(current::endsWith)) {
                    stringBuilder.append(" ");
                }
                if (Stream.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",
                        "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y",
                        "z").anyMatch(l -> current.toLowerCase().endsWith(l))) {
                    stringBuilder.append(" ");
                }
            }
        });
        String fixedContent = stringBuilder.toString().replace("\n\n", "\n").replace("  ", " ");
        return insertSpaceBetweenChEn(fixedContent);
    }
}
