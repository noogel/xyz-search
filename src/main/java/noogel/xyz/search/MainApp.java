package noogel.xyz.search;

import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;
import noogel.xyz.search.infrastructure.repo.impl.lucene.LuceneFullTextSearchRepoImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MainApp {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(MainApp.class, args);
//        LuceneFullTextSearchRepoImpl bean = ctx.getBean(LuceneFullTextSearchRepoImpl.class);
//        bean.getLuceneWriter().write(new FullTextSearchModel());
    }
}
 