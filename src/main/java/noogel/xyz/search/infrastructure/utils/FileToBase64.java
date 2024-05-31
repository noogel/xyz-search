package noogel.xyz.search.infrastructure.utils;

import java.util.Base64;

public class FileToBase64 {
    public static String conversion(byte[] fileContent) {
        return Base64
                .getEncoder()
                .encodeToString(fileContent);
    }
}
