package com.wildbeyond.controller;

import com.wildbeyond.dto.BlogArticleView;
import com.wildbeyond.dto.ExploreEntryView;
import com.wildbeyond.dto.WildlifePhotoView;
import com.wildbeyond.service.HomepageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomepageService homepageService;

    @GetMapping("/")
    public String home(Model model) {
        populateHomeModel(model);
        return "home";
    }

    @GetMapping({"/home", "/index"})
    public String homeAlias() {
        return "redirect:/";
    }

    @GetMapping("/blog")
    public String blog(Model model) {
        var articles = homepageService.getBlogFeed();
        model.addAttribute("articles", articles);
        model.addAttribute("featuredArticle", articles.isEmpty() ? null : articles.get(0));
        model.addAttribute("editorialPicks", articles.size() > 1 ? articles.subList(1, Math.min(5, articles.size())) : Collections.emptyList());
        model.addAttribute("visualStories", articles.size() > 5 ? articles.subList(5, Math.min(11, articles.size())) : Collections.emptyList());

        // Compatibility with previous template/tests.
        model.addAttribute("blogs", homepageService.getAllPublishedBlogs());
        return "blog";
    }

    @GetMapping("/explore")
    public String explore(Model model) {
        var categories = homepageService.getExploreEntriesByCategory();
        model.addAttribute("animals", categories.getOrDefault("animals", List.of()));
        model.addAttribute("birds", categories.getOrDefault("birds", List.of()));
        model.addAttribute("ecosystems", categories.getOrDefault("ecosystems", List.of()));
        return "explore";
    }

    @GetMapping("/explore/wildlife-photography")
    public String wildlifePhotography(Model model) {
        model.addAttribute("photos", homepageService.getWildlifePhotographyGallery());
        return "wildlife-photography";
    }

    @GetMapping("/explore/{category}/{slug}")
    public String exploreDetail(@PathVariable String category,
                                @PathVariable String slug,
                                Model model) {
        var entry = homepageService.findExploreEntry(category, slug)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Explore entry not found"));
        model.addAttribute("entry", entry);
        model.addAttribute("entryNarrative", buildEntryNarrative(entry));
        return "explore-detail";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/blog/{id}")
    public String blogDetail(@PathVariable Long id, Model model) {
        var post = homepageService.findBlogPostById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Blog post not found"));
        model.addAttribute("post", post);
        return "blog-detail";
    }

    private void populateHomeModel(Model model) {
        var blogs = homepageService.getFeaturedBlogs();
        var products = homepageService.getFeaturedProducts();
        var categories = homepageService.getExploreEntriesByCategory();
        if (categories == null) {
            categories = Map.of();
        }

        var animals = categories.getOrDefault("animals", List.of());
        var birds = categories.getOrDefault("birds", List.of());
        var ecosystems = categories.getOrDefault("ecosystems", List.of());

        var articleFeed = homepageService.getBlogFeed();
        if (articleFeed == null) {
            articleFeed = List.of();
        }

        var wildlifePhotos = homepageService.getWildlifePhotographyGallery();
        if (wildlifePhotos == null) {
            wildlifePhotos = List.of();
        }

        List<StoryView> stories = buildLatestStories(articleFeed, animals, ecosystems);
        if (stories.isEmpty()) {
            stories = List.of(new StoryView(
                    "African Elephant",
                    "Conservation field snapshots from migration corridors and local ranger checkpoints.",
                    "/images/deer-hero.jpg",
                    "Field Dispatch",
                    "/explore/animals/african-elephant"
            ));
        }

        StoryView featuredStory = stories.get(0);

        List<StoryView> documentaries = buildDocumentaryShowcase(animals, birds, wildlifePhotos);

        StoryView travelFeature = buildTravelFeature(ecosystems, articleFeed);

        List<StoryView> travelStories = buildTravelStories(ecosystems, articleFeed);
        List<StoryView> issueArticles = buildIssueStories(articleFeed, birds, ecosystems);

        String issueCoverImage = issueArticles.isEmpty() ? "/images/deer-hero.jpg" : issueArticles.get(0).image();
        String featureBannerImage = travelFeature.image();
        String missionImageOne = animals.isEmpty() ? "/images/deer-hero.jpg" : animals.get(0).getImageUrl();
        String missionImageTwo = birds.isEmpty() ? "/images/deer-hero.jpg" : birds.get(0).getImageUrl();
        String missionImageThree = ecosystems.isEmpty() ? "/images/deer-hero.jpg" : ecosystems.get(0).getImageUrl();
        String featuredShopFallbackImage = ecosystems.size() > 1 ? ecosystems.get(1).getImageUrl() : "/images/deer-hero.jpg";

        model.addAttribute("blogs", blogs);
        model.addAttribute("products", products);
        model.addAttribute("stories", stories);
        model.addAttribute("featuredStory", featuredStory);
        model.addAttribute("travelFeature", travelFeature);
        model.addAttribute("documentaries", documentaries);
        model.addAttribute("travelStories", travelStories);
        model.addAttribute("issueArticles", issueArticles);
        model.addAttribute("issueCoverImage", issueCoverImage);
        model.addAttribute("featureBannerImage", featureBannerImage);
        model.addAttribute("missionImageOne", missionImageOne);
        model.addAttribute("missionImageTwo", missionImageTwo);
        model.addAttribute("missionImageThree", missionImageThree);
        model.addAttribute("featuredShopFallbackImage", featuredShopFallbackImage);
    }

    private List<StoryView> buildLatestStories(List<BlogArticleView> articleFeed,
                                               List<ExploreEntryView> animals,
                                               List<ExploreEntryView> ecosystems) {
        List<StoryView> result = new ArrayList<>();

        articleFeed.stream()
                .limit(2)
                .map(article -> toBlogStory(article, "Global Feed"))
                .forEach(result::add);

        ecosystems.stream()
                .limit(1)
                .map(entry -> toExploreStory(entry, "Ecosystem"))
                .forEach(result::add);

        animals.stream()
                .limit(1)
                .map(entry -> toExploreStory(entry, "Wildlife"))
                .forEach(result::add);

        return result;
    }

    private List<StoryView> buildDocumentaryShowcase(List<ExploreEntryView> animals,
                                                     List<ExploreEntryView> birds,
                                                     List<WildlifePhotoView> photos) {
        List<StoryView> result = new ArrayList<>();

        animals.stream().limit(2).map(entry -> toExploreStory(entry, "Wildlife Documentary")).forEach(result::add);
        birds.stream().limit(1).map(entry -> toExploreStory(entry, "Wildlife Documentary")).forEach(result::add);

        String galleryImage = photos.isEmpty() || photos.get(0).getImageUrl() == null || photos.get(0).getImageUrl().isBlank()
                ? "/images/lion.jpg"
                : photos.get(0).getImageUrl();
        result.add(new StoryView(
                "Wildlife Photography Archive",
                "Visual field captures with zoomable high-resolution frames and habitat-linked notes.",
                galleryImage,
                "Documentary",
                "/explore/wildlife-photography"
        ));

        return result;
    }

    private StoryView buildTravelFeature(List<ExploreEntryView> ecosystems,
                                         List<BlogArticleView> articleFeed) {
        if (!ecosystems.isEmpty()) {
            return toExploreStory(ecosystems.get(0), "Travel Feature");
        }

        List<BlogArticleView> travelArticles = filterTravelArticles(articleFeed);
        if (!travelArticles.isEmpty()) {
            return toBlogStory(travelArticles.get(0), "Travel Feature");
        }

        return new StoryView(
                "River Delta",
                "A practical field route for travelers, students, and citizen observers to understand sediment flow, mangrove edges, and conservation checkpoints.",
                "/images/deer-hero.jpg",
                "Travel Feature",
                "/explore"
        );
    }

    private List<StoryView> buildTravelStories(List<ExploreEntryView> ecosystems,
                                               List<BlogArticleView> articleFeed) {
        List<StoryView> result = new ArrayList<>();
        Set<String> usedHrefs = new HashSet<>();

        ecosystems.stream()
                .skip(1)
                .limit(3)
                .map(entry -> toExploreStory(entry, "Travel"))
                .forEach(story -> {
                    result.add(story);
                    usedHrefs.add(story.href());
                });

        for (var article : filterTravelArticles(articleFeed)) {
            StoryView mapped = toBlogStory(article, "Travel Story");
            if (!usedHrefs.contains(mapped.href())) {
                result.add(mapped);
                usedHrefs.add(mapped.href());
            }
            if (result.size() >= 4) {
                break;
            }
        }

        return result;
    }

    private List<StoryView> buildIssueStories(List<BlogArticleView> articleFeed,
                                              List<ExploreEntryView> birds,
                                              List<ExploreEntryView> ecosystems) {
        List<StoryView> result = new ArrayList<>();

        articleFeed.stream()
                .limit(2)
                .map(article -> toBlogStory(article, "Issue Brief"))
                .forEach(result::add);

        ecosystems.stream().limit(1).map(entry -> toExploreStory(entry, "Issue")).forEach(result::add);
        birds.stream().limit(1).map(entry -> toExploreStory(entry, "Issue")).forEach(result::add);

        return result;
    }

    private List<BlogArticleView> filterTravelArticles(List<BlogArticleView> articleFeed) {
        if (articleFeed == null || articleFeed.isEmpty()) {
            return List.of();
        }

        List<String> keywords = List.of("travel", "journey", "expedition", "route", "trek", "destination", "corridor");
        List<BlogArticleView> filtered = new ArrayList<>();

        for (var article : articleFeed) {
            String text = (article.getTitle() + " " + (article.getSummary() == null ? "" : article.getSummary())).toLowerCase(Locale.ROOT);
            boolean matched = keywords.stream().anyMatch(text::contains);
            if (matched) {
                filtered.add(article);
            }
        }

        if (filtered.isEmpty()) {
            return articleFeed.stream().limit(4).toList();
        }
        return filtered;
    }

    private StoryView toBlogStory(BlogArticleView article, String category) {
        String image = article.getImageUrl();
        if (image == null || image.isBlank()) {
            image = "/images/deer-hero.jpg";
        }

        String summary = article.getSummary();
        if (summary == null || summary.isBlank()) {
            summary = "Wildlife update from current field reporting.";
        }

        return new StoryView(
                article.getTitle(),
                shorten(summary, 180),
                image,
                category,
                resolveArticleHref(article)
        );
    }

    private String resolveArticleHref(BlogArticleView article) {
        if (article.isInternal()) {
            return "/blog/" + article.getLocalId();
        }
        if (article.getSourceUrl() != null && !article.getSourceUrl().isBlank()) {
            return article.getSourceUrl();
        }
        return "/blog";
    }

    private String shorten(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private StoryView toExploreStory(ExploreEntryView entry, String category) {
        return new StoryView(
                entry.getName(),
                entry.getShortDescription() == null || entry.getShortDescription().isBlank()
                        ? "A field note from the wild."
                        : entry.getShortDescription(),
                entry.getImageUrl() == null || entry.getImageUrl().isBlank() ? "/images/deer-hero.jpg" : entry.getImageUrl(),
                category,
                "/explore/" + entry.getCategory() + "/" + entry.getSlug()
        );
    }

    private List<String> buildEntryNarrative(ExploreEntryView entry) {
        List<String> narrative = new ArrayList<>();
        narrative.add("Habitat focus: " + entry.getHabitat() + ". Current conservation status: " + entry.getConservationStatus() + ". Protecting this landscape requires long-term monitoring, local stewardship, and consistent policy support.");

        String category = entry.getCategory() == null ? "" : entry.getCategory().toLowerCase();
        switch (category) {
            case "animals" -> {
                narrative.add("Population trends for " + entry.getName() + " often reveal deeper ecological pressures, from habitat fragmentation to prey imbalance. Conservation plans work best when protected corridors are connected with community-led conflict prevention.");
                narrative.add("Field teams track breeding success, mortality hotspots, and movement behavior to guide adaptive action each season. That means this profile is not just reference information, but a practical snapshot of where restoration investment matters most.");
            }
            case "birds" -> {
                narrative.add("Bird populations react quickly to water quality, food-web shifts, and land-use change, so they are strong early indicators of ecosystem health. Monitoring nesting success and migration timing helps identify stress before wider collapse happens.");
                narrative.add("For " + entry.getName() + ", protecting stopover habitat and reducing disturbance around breeding zones can significantly improve long-term resilience. Coordinated flyway-scale action is often more effective than isolated local interventions.");
            }
            case "ecosystems" -> {
                narrative.add("This ecosystem supports climate regulation, biodiversity stability, and local livelihoods at the same time. When one function weakens, the others usually follow, which is why restoration plans now combine water, carbon, and species targets together.");
                narrative.add("In practice, successful ecosystem recovery depends on measurable baselines, multi-year financing, and participation from communities directly managing land and water resources. This page summarizes the core context so visitors can understand value without leaving the site.");
            }
            default -> {
                narrative.add("Long-term biodiversity protection depends on combining scientific monitoring with local knowledge, practical policy, and sustained conservation finance.");
            }
        }

        return narrative;
    }

    public record StoryView(String title, String summary, String image, String category, String href) {
    }
}
