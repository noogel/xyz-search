package noogel.xyz.search.infrastructure.utils;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * HTML模板渲染工具类
 */
public class HTMLTemplateHelper {

    private final static TemplateEngine templateEngine = new TemplateEngine();

    /**
     * 使用 Thymeleaf 渲染 HTML
     *
     * @param template HTML模板
     * @param params   参数
     * @return 渲染后的HTML
     */
    public static String render(String template, Map<String, Object> params) {
        Resource resource = new DefaultResourceLoader().getResource("classpath:templates/" + template);
        try (InputStream inputStream = resource.getInputStream()) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] b = new byte[10240];
                int n;
                while ((n = inputStream.read(b)) != -1) {
                    outputStream.write(b, 0, n);
                }
                String readString = outputStream.toString();
                Context context = new Context();
                context.setVariables(params);
                return templateEngine.process(readString, context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
