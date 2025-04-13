package noogel.xyz.search.infrastructure.utils;

public class AuthorHelper {
    public static String format(String author) {
        return author.replace("\"", "")
                .replace("（", "(")
                .replace("：", ":")
                .replace("）", ")")
                .replace("//", "_")
                .trim();
    }
}
