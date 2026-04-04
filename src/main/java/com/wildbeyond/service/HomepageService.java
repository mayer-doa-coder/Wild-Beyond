package com.wildbeyond.service;

import com.wildbeyond.dto.BlogArticleView;
import com.wildbeyond.model.BlogPost;
import com.wildbeyond.model.Product;
import com.wildbeyond.repository.BlogPostRepository;
import com.wildbeyond.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class HomepageService {

    private static final String DEFAULT_BLOG_IMAGE = "/images/lion.jpg";

    private final BlogPostRepository blogRepo;
    private final ProductRepository productRepo;

    @Value("${GUARDIAN_API_KEY:}")
    private String guardianApiKey;

    @Value("${NEWSAPI_KEY:}")
    private String newsApiKey;

    @Value("${external.blog.request-timeout-seconds:8}")
    private long requestTimeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public List<BlogPost> getFeaturedBlogs() {
        return blogRepo.findTop5ByPublishedTrueOrderByCreatedAtDesc();
    }

    public List<BlogPost> getAllPublishedBlogs() {
        return blogRepo.findByPublishedTrueOrderByCreatedAtDesc();
    }

    public List<Product> getFeaturedProducts() {
        return productRepo.findTop5ByOrderByIdDesc();
    }

    public Optional<BlogPost> findBlogPostById(Long id) {
        return blogRepo.findByIdAndPublishedTrue(id);
    }

    public List<BlogArticleView> getBlogFeed() {
        Map<String, BlogArticleView> deduped = new LinkedHashMap<>();

        for (BlogArticleView article : fromGuardian()) {
            deduped.putIfAbsent(article.getTitle().toLowerCase(), article);
        }
        for (BlogArticleView article : fromNewsApi()) {
            deduped.putIfAbsent(article.getTitle().toLowerCase(), article);
        }

        if (deduped.size() < 6) {
            for (BlogArticleView article : fromInternalPosts()) {
                deduped.putIfAbsent(article.getTitle().toLowerCase(), article);
            }
        }

        return deduped.values().stream().limit(18).toList();
    }

    private List<BlogArticleView> fromInternalPosts() {
        return getAllPublishedBlogs().stream().map(post -> BlogArticleView.builder()
                        .localId(post.getId())
                        .title(post.getTitle())
                        .summary(post.getContent() == null || post.getContent().isBlank()
                                ? "Conservation update from the field."
                                : abbreviate(post.getContent(), 240))
                        .imageUrl(DEFAULT_BLOG_IMAGE)
                        .sourceName("Wild Beyond")
                        .sourceUrl(null)
                        .publishedAt(post.getCreatedAt())
                        .build())
                .toList();
    }

    private List<BlogArticleView> fromGuardian() {
        if (guardianApiKey == null || guardianApiKey.isBlank()) {
            return List.of();
        }

        try {
            String query = URLEncoder.encode("wildlife conservation", StandardCharsets.UTF_8);
            String url = "https://content.guardianapis.com/search?api-key=" + guardianApiKey
                    + "&show-fields=trailText,thumbnail"
                    + "&page-size=12"
                    + "&q=" + query;
            JsonNode root = getJson(url);
            JsonNode results = root.path("response").path("results");
            if (!results.isArray()) {
                return List.of();
            }

            List<BlogArticleView> feed = new ArrayList<>();
            for (JsonNode result : results) {
                String title = result.path("webTitle").asText("");
                if (title.isBlank()) {
                    continue;
                }

                JsonNode fields = result.path("fields");
                String summary = fields.path("trailText").asText("Wildlife field update.");
                String image = fields.path("thumbnail").asText(DEFAULT_BLOG_IMAGE);
                String link = result.path("webUrl").asText(null);
                LocalDateTime publishedAt = parseDate(result.path("webPublicationDate").asText(null));

                feed.add(BlogArticleView.builder()
                        .localId(null)
                        .title(title)
                        .summary(summary)
                        .imageUrl(image == null || image.isBlank() ? DEFAULT_BLOG_IMAGE : image)
                        .sourceName("The Guardian")
                        .sourceUrl(link)
                        .publishedAt(publishedAt)
                        .build());
            }
            return feed;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<BlogArticleView> fromNewsApi() {
        if (newsApiKey == null || newsApiKey.isBlank()) {
            return List.of();
        }

        try {
            String query = URLEncoder.encode("wildlife conservation", StandardCharsets.UTF_8);
            String url = "https://newsapi.org/v2/everything?q=" + query
                    + "&language=en&pageSize=12&sortBy=publishedAt&apiKey=" + newsApiKey;
            JsonNode root = getJson(url);
            JsonNode articles = root.path("articles");
            if (!articles.isArray()) {
                return List.of();
            }

            List<BlogArticleView> feed = new ArrayList<>();
            for (JsonNode article : articles) {
                String title = article.path("title").asText("");
                if (title.isBlank() || "[Removed]".equalsIgnoreCase(title)) {
                    continue;
                }

                String summary = article.path("description").asText("Wildlife field update.");
                String image = article.path("urlToImage").asText(DEFAULT_BLOG_IMAGE);
                String source = article.path("source").path("name").asText("NewsAPI");
                String link = article.path("url").asText(null);
                LocalDateTime publishedAt = parseDate(article.path("publishedAt").asText(null));

                feed.add(BlogArticleView.builder()
                        .localId(null)
                        .title(title)
                        .summary(summary)
                        .imageUrl(image == null || image.isBlank() ? DEFAULT_BLOG_IMAGE : image)
                        .sourceName(source)
                        .sourceUrl(link)
                        .publishedAt(publishedAt)
                        .build());
            }
            return feed;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(Math.max(2, requestTimeoutSeconds)))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Failed blog API response");
        }
        return objectMapper.readTree(response.body());
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 3) + "...";
    }
}
