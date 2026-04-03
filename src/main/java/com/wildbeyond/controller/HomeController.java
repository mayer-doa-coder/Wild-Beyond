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

    @GetMapping("/blog")
    public String blog(Model model) {
        populateHomeModel(model);
        return "home";
    }

    @GetMapping("/explore")
    public String explore(Model model) {
        populateHomeModel(model);
        return "home";
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

        List<StoryView> stories = new ArrayList<>();

        for (var blog : blogs) {
            stories.add(new StoryView(
                blog.getTitle(),
                blog.getContent() == null || blog.getContent().isBlank()
                    ? "A field note from the wild."
                    : blog.getContent(),
                "/images/lion.jpg",
                "Wildlife",
                "/blog/" + blog.getId()
            ));
        }

        if (stories.isEmpty()) {
            stories = List.of(
                new StoryView("Rainforest Signals", "Hidden indicators reveal ecosystem recovery patterns.", "/images/lion.jpg", "Ecology", "/explore"),
                new StoryView("Desert Water Routes", "Migratory species are reshaping survival maps.", "/images/lion.jpg", "Climate", "/explore"),
                new StoryView("Mountain Patrol", "Conservation teams track habitats through winter corridors.", "/images/lion.jpg", "Conservation", "/explore")
            );
        }

        StoryView featuredStory = stories.get(0);

        List<StoryView> documentaries = List.of(
            new StoryView("Giants of the Floodplain", "River life under seasonal extremes.", "/images/lion.jpg", "Documentary", "/explore"),
            new StoryView("Predators at Dusk", "Nocturnal hunters and adaptation.", "/images/lion.jpg", "Documentary", "/explore"),
            new StoryView("Beyond the Canopy", "Forest layers and secret species.", "/images/lion.jpg", "Documentary", "/explore"),
            new StoryView("Arctic Frontier", "Survival in shifting ice worlds.", "/images/lion.jpg", "Documentary", "/explore")
        );

        List<StoryView> travelStories = List.of(
            new StoryView("Journey Through Wetlands", "Navigating biodiversity corridors by canoe.", "/images/lion.jpg", "Travel", "/explore"),
            new StoryView("Ridgeline Trails", "Tracking alpine wildlife migration paths.", "/images/lion.jpg", "Travel", "/explore"),
            new StoryView("Savannah Nights", "Field camps under star-driven navigation.", "/images/lion.jpg", "Travel", "/explore")
        );

        List<StoryView> issueArticles = List.of(
            new StoryView("Engineering Natural Fibers", "Bio-inspired materials for conservation gear.", "/images/lion.jpg", "Issue", "/blog"),
            new StoryView("Tracking Ocean Heat", "How marine ecosystems respond to thermal stress.", "/images/lion.jpg", "Issue", "/blog"),
            new StoryView("Rewilding Urban Edges", "Designing city borders for biodiversity.", "/images/lion.jpg", "Issue", "/blog"),
            new StoryView("Skyline Migrants", "Bird migration routes across megacities.", "/images/lion.jpg", "Issue", "/blog")
        );

        model.addAttribute("blogs", blogs);
        model.addAttribute("products", products);
        model.addAttribute("stories", stories);
        model.addAttribute("featuredStory", featuredStory);
        model.addAttribute("documentaries", documentaries);
        model.addAttribute("travelStories", travelStories);
        model.addAttribute("issueArticles", issueArticles);
    }

        public record StoryView(String title, String summary, String image, String category, String href) {
        }
}
