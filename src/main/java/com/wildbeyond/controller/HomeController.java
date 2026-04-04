package com.wildbeyond.controller;

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
import java.util.List;

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
        var animals = categories.getOrDefault("animals", List.of());
        var birds = categories.getOrDefault("birds", List.of());
        var ecosystems = categories.getOrDefault("ecosystems", List.of());
        var articleFeed = homepageService.getBlogFeed();

        List<StoryView> stories = articleFeed.stream()
                .limit(4)
                .map(article -> new StoryView(
                        article.getTitle(),
                        article.getSummary() == null || article.getSummary().isBlank()
                                ? "A field note from the wild."
                                : article.getSummary(),
                        article.getImageUrl() == null || article.getImageUrl().isBlank()
                                ? "/images/deer-hero.jpg"
                                : article.getImageUrl(),
                        article.isInternal() ? "Wild Beyond" : "Field Report",
                        article.isInternal()
                                ? "/blog/" + article.getLocalId()
                                : (article.getSourceUrl() == null || article.getSourceUrl().isBlank() ? "/blog" : article.getSourceUrl())
                ))
                .toList();

        if (stories.isEmpty()) {
            List<StoryView> fallbackStories = new ArrayList<>();
            for (var entry : animals.stream().limit(3).toList()) {
                fallbackStories.add(toExploreStory(entry, "Ecology"));
            }
            stories = fallbackStories.isEmpty()
                    ? List.of(new StoryView("Rainforest Signals", "Hidden indicators reveal ecosystem recovery patterns.", "/images/deer-hero.jpg", "Ecology", "/explore"))
                    : fallbackStories;
        }

        StoryView featuredStory = stories.get(0);

        List<StoryView> documentaries = new ArrayList<>();
        for (var entry : animals.stream().limit(2).toList()) {
            documentaries.add(toExploreStory(entry, "Documentary"));
        }
        for (var entry : birds.stream().limit(2).toList()) {
            documentaries.add(toExploreStory(entry, "Documentary"));
        }

        if (documentaries.isEmpty()) {
            documentaries = List.of(new StoryView("Wildlife Field Logs", "Visual records from active conservation landscapes.", "/images/deer-hero.jpg", "Documentary", "/explore"));
        }

        List<StoryView> travelStories = ecosystems.stream()
                .limit(3)
                .map(entry -> toExploreStory(entry, "Travel"))
                .toList();

        if (travelStories.isEmpty()) {
            travelStories = List.of(new StoryView("Journey Through Wetlands", "Navigating biodiversity corridors by canoe.", "/images/deer-hero.jpg", "Travel", "/explore"));
        }

        List<StoryView> issueArticles = articleFeed.stream()
                .filter(article -> article.isInternal())
                .limit(4)
                .map(article -> new StoryView(
                        article.getTitle(),
                        article.getSummary() == null || article.getSummary().isBlank() ? "Field issue update." : article.getSummary(),
                        article.getImageUrl() == null || article.getImageUrl().isBlank() ? "/images/deer-hero.jpg" : article.getImageUrl(),
                        "Issue",
                        "/blog/" + article.getLocalId()
                ))
                .toList();

        String issueCoverImage = ecosystems.isEmpty() ? "/images/deer-hero.jpg" : ecosystems.get(0).getImageUrl();
        String featureBannerImage = animals.size() > 1 ? animals.get(1).getImageUrl() : "/images/deer-hero.jpg";
        String missionImageOne = animals.isEmpty() ? "/images/deer-hero.jpg" : animals.get(0).getImageUrl();
        String missionImageTwo = birds.isEmpty() ? "/images/deer-hero.jpg" : birds.get(0).getImageUrl();
        String missionImageThree = ecosystems.isEmpty() ? "/images/deer-hero.jpg" : ecosystems.get(0).getImageUrl();
        String featuredShopFallbackImage = ecosystems.size() > 1 ? ecosystems.get(1).getImageUrl() : "/images/deer-hero.jpg";

        model.addAttribute("blogs", blogs);
        model.addAttribute("products", products);
        model.addAttribute("stories", stories);
        model.addAttribute("featuredStory", featuredStory);
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

    private StoryView toExploreStory(com.wildbeyond.dto.ExploreEntryView entry, String category) {
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

        public record StoryView(String title, String summary, String image, String category, String href) {
        }
}
