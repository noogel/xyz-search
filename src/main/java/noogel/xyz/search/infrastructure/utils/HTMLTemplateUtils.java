package noogel.xyz.search.infrastructure.utils;

import org.springframework.util.ResourceUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

/**
 * HTML模板渲染工具类
 */
public class HTMLTemplateUtils {

    private final static TemplateEngine templateEngine = new TemplateEngine();

    /**
     * 使用 Thymeleaf 渲染 HTML
     * @param template  HTML模板
     * @param params 参数
     * @return  渲染后的HTML
     */
    public static String render(String template, Map<String, Object> params){
        try {
            File file = ResourceUtils.getFile("classpath:templates/" + template);
            String readString = Files.readString(file.toPath());
            Context context = new Context();
            context.setVariables(params);
            return templateEngine.process(readString, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
