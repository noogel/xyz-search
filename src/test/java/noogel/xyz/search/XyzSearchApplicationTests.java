package noogel.xyz.search;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.SyndFeedOutput;
import com.rometools.rome.io.XmlReader;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URL;

@SpringBootTest
class XyzSearchApplicationTests {

    @SneakyThrows
    @Test
    void contextLoads() {
        String url = "https://tatsu-zine.com/catalogs.opds";
        SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
        System.out.println(feed);

        SyndFeed feed1 = new SyndFeedImpl();
        feed1.setFeedType("rss_2.0");
        feed1.setTitle("test-title");
        feed1.setDescription("test-description");
        feed1.setLink("https://example.org");
        System.out.println("====================");
        System.out.println(new SyndFeedOutput().outputString(feed1));
    }

}
