package crawler.novel;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import crawler.novel.model.Book;
import crawler.novel.service.BookService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class NovelApplication implements CommandLineRunner{
    @Autowired
    private BookService bookService;

    public static void main(String[] args) throws IOException {
        SpringApplication.run(NovelApplication.class, args);
    }

    public void LoadContentAndSave(String url, Book book) throws IOException, InterruptedException {
        HtmlPage page;
        Document doc;
        WebClient wc = new WebClient(BrowserVersion.CHROME);
        wc.getOptions().setUseInsecureSSL(true);
        wc.getOptions().setJavaScriptEnabled(true);
        wc.getOptions().setCssEnabled(false);
        wc.getOptions().setThrowExceptionOnScriptError(false);
        wc.getOptions().setTimeout(100000);
        wc.getOptions().setDoNotTrackEnabled(false);
        page = wc.getPage(url);
        Thread.sleep(3000);
        doc = Jsoup.parse(page.asXml());
        book.setContent(doc.select("#content").first().html());
        bookService.save(book);
    }

    @Override
    public void run(String... args) throws Exception {
        int locElementsSize;

        Stream<String> urls = Jsoup.parse(ResourceUtils.getFile("classpath:sitemap.xml"),"utf-8")
                .select("url>loc").stream().map(x -> x.text() + "list.html");

        urls.parallel().forEach(url -> {
            try {
                process(url);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void process(String url) throws IOException, InterruptedException {
        Document bookDocument = Jsoup.connect(url).get();

        String name = bookDocument.select("h3").text().trim();
        String type = bookDocument.select("span[itemprop='category']").text().trim();
        String state = bookDocument.select("div.list2").first().select("span").last().text().trim();
        Elements directoryElements = bookDocument.select("li[itemprop='itemListElement']");

        for(int j = 0; j < directoryElements.size(); j++){
            String directoryURL = directoryElements.get(j).select("a").first().absUrl("href");
            String directoryName = directoryElements.get(j).select("span[itemprop='name']").first().text().trim();
            Book book = new Book(name, directoryName, type, j + 1, "", state);
            LoadContentAndSave(directoryURL, book);
        }
    }
}
