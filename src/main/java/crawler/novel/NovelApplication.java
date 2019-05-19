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
        Book book;
        Elements locElements;
        int locElementsSize;
        Document bookDocument;
        String name;
        String type;
        String state;
        Elements directoryElements;
        String directoryURL;
        String directoryName;

        locElements = Jsoup.parse(ResourceUtils.getFile("classpath:sitemap.xml"),"utf-8").select("url>loc");
        locElementsSize = locElements.size();
        for(int i = 1; i < locElementsSize; i++){
            bookDocument = Jsoup.connect(locElements.get(i).text() + "list.html").get();
            name = bookDocument.select("h3").text();
            type = bookDocument.select("span[itemprop='category']").text();
            state = bookDocument.select("div.list2").first().select("span").last().text();
            directoryElements = bookDocument.select("li[itemprop='itemListElement']");
            for(int j = 0; j < directoryElements.size(); j++){
                directoryURL = directoryElements.get(j).select("a").first().absUrl("href");
                directoryName = directoryElements.get(j).select("span[itemprop='name']").first().text();
                book = new Book(name, directoryName, type, j + 1, "", state);
                LoadContentAndSave(directoryURL, book);
            }
        }
    }
}
