package com.ai.generation.trendprocessing;

import com.ai.generation.exception.TrendProcessingException;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleTrendsService implements TrendService {

    private static final String URL = "https://trends.google.com/trending/rss?geo=US";
    private static final int MAX_ITEMS = 40;

    public List<String> getTrends() {
        List<String> trends = new ArrayList<>();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new TrendProcessingException("Failed to fetch Google Trends: HTTP " + response.statusCode());
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(
                    new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8)));

            NodeList items = doc.getElementsByTagName("item");
            int limit = Math.min(items.getLength(), MAX_ITEMS);

            for (int i = 0; i < limit; i++) {
                Element item = (Element) items.item(i);
                NodeList titleNodes = item.getElementsByTagName("title");
                if (titleNodes.getLength() > 0) {
                    String title = titleNodes.item(0).getTextContent();
                    if (title != null && !title.isBlank()) {
                        trends.add(title);
                    }
                }
            }

        } catch (Exception e) {
            throw new TrendProcessingException("Google Trends service unavailable", e);
        }

        return trends;
    }
}
