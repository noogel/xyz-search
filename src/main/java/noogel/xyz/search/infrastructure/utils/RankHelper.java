package noogel.xyz.search.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RankHelper {
    private static final Pattern DIG = Pattern.compile("(\\d+)");

    public static long calcRank(String name) {
        try {
            return calc(name);
        } catch (Exception ex) {
            log.error("RankHelper {}", name, ex);
            return 0;
        }
    }

    private static long calc(String name) {
        if (null == name || name.length() <= 0) {
            return Long.MAX_VALUE;
        }
        long sort = 0;
        Matcher matcher = DIG.matcher(name);

        if (matcher.find()) {
            String num = matcher.group(0);
            // 数字在前半部分
            int start = name.indexOf(num);
            if (start <= (name.length() / 2 + 1)) {
                long n = Long.parseUnsignedLong(num);
                // 大于 0x7FFFL 按 0x7FFFL 算。最多算 15 位。
                n = n > 0x7FFFL ? 0x7FFFL : n;
                // 放置前到前 16 位。
                sort |= n << 48;
                name = name.substring(0, start) + name.substring(start + num.length());
            }
        } else {
            sort |= 0x7FFFL << 48;
        }
        // 后续字节填充
        for (int i = 0; i < 6; i++) {
            if (i >= name.length()) {
                continue;
            }
            sort |= (Byte.toUnsignedLong((byte) name.charAt(i)) << (8 * (5 - i)));
        }
        return sort;
    }
}
