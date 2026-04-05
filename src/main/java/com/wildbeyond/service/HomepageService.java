package com.wildbeyond.service;

import com.wildbeyond.dto.BlogArticleView;
import com.wildbeyond.dto.ExploreEntryView;
import com.wildbeyond.dto.WildlifePhotoView;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private final Map<String, String> wikiImageCache = new ConcurrentHashMap<>();

    private static final List<ExploreEntryView> EXPLORE_ENTRIES = List.of(
                entry("animals", "african-elephant", "African Elephant", "The largest land mammal and ecosystem engineer.", "African elephants open migration paths, disperse seeds over long distances, and create water access points used by many other species. Their presence supports biodiversity across entire savannah systems.", "https://images.unsplash.com/photo-1557050543-4d5f4e07ef46?auto=format&fit=crop&w=1400&q=80", "50% 38%", "Savannah, woodland", "Endangered", "https://www.worldwildlife.org/species/african-elephant"),
                entry("animals", "snow-leopard", "Snow Leopard", "A high-altitude predator of cold mountain ranges.", "Snow leopards regulate prey populations in alpine ecosystems and indicate healthy mountain food webs. Their wide range needs protected corridors and community-supported coexistence plans.", "https://images.unsplash.com/photo-1552410260-0fd9b577afa6?auto=format&fit=crop&w=1400&q=80", "50% 40%", "Alpine mountains", "Vulnerable", "https://www.worldwildlife.org/species/snow-leopard"),
                entry("animals", "bengal-tiger", "Bengal Tiger", "An apex predator critical to forest balance.", "Bengal tigers help stabilize herbivore populations and keep forest ecosystems resilient. Their conservation protects large landscapes that also store carbon and support river systems.", "https://images.unsplash.com/photo-1546182990-dffeafbe841d?auto=format&fit=crop&w=1400&q=80", "50% 32%", "Tropical and subtropical forests", "Endangered", "https://www.worldwildlife.org/species/tiger"),
                entry("animals", "red-fox", "Red Fox", "A highly adaptable predator in mixed landscapes.", "Red foxes thrive in grasslands, forests, and urban edges. Their adaptability makes them useful indicators of habitat fragmentation and prey availability.", "https://images.unsplash.com/photo-1474511016485-2f2c687d9d6f?auto=format&fit=crop&w=1400&q=80", "50% 45%", "Woodland and grassland", "Least Concern", "https://www.nationalgeographic.com/animals/mammals/facts/red-fox"),
                entry("animals", "gray-wolf", "Gray Wolf", "A social predator that restores trophic balance.", "Gray wolves shape prey behavior and reduce overgrazing pressure in many landscapes. Their return can trigger ecosystem recovery from riverbanks to forest regeneration.", "https://images.unsplash.com/photo-1549366021-9f761d450615?auto=format&fit=crop&w=1400&q=80", "50% 45%", "Forests and tundra", "Least Concern", "https://www.worldwildlife.org/species/gray-wolf"),
                entry("animals", "cheetah", "Cheetah", "The fastest land animal with shrinking range.", "Cheetahs rely on open habitats and wide movement corridors. Fragmentation and conflict reduce survival, making cross-boundary conservation essential.", "https://images.unsplash.com/photo-1526318472351-c75fcf070305?auto=format&fit=crop&w=1400&q=80", "50% 40%", "Savannah and grassland", "Vulnerable", "https://www.worldwildlife.org/species/cheetah"),
                entry("animals", "orangutan", "Orangutan", "A forest canopy specialist and seed disperser.", "Orangutans maintain rainforest dynamics through seed dispersal and canopy movement. Their decline signals severe forest degradation and peatland loss.", "https://images.unsplash.com/photo-1564349683136-77e08dba1ef7?auto=format&fit=crop&w=1400&q=80", "50% 35%", "Tropical rainforests", "Critically Endangered", "https://www.worldwildlife.org/species/orangutan"),
                entry("animals", "polar-bear", "Polar Bear", "An Arctic predator dependent on sea ice.", "Polar bears are strongly affected by sea-ice decline, which reduces hunting opportunities and increases energetic stress. Their status reflects broader Arctic climate change.", "https://images.unsplash.com/photo-1517783999520-f068d7431a60?auto=format&fit=crop&w=1400&q=80", "50% 42%", "Arctic coasts and ice", "Vulnerable", "https://www.worldwildlife.org/species/polar-bear"),
                entry("animals", "mountain-gorilla", "Mountain Gorilla", "A conservation recovery story still needing vigilance.", "Mountain gorillas have rebounded through anti-poaching patrols, habitat protection, and veterinary intervention. Continued community partnership remains essential.", "https://images.unsplash.com/photo-1559253664-ca249d4608c3?auto=format&fit=crop&w=1400&q=80", "50% 28%", "Montane forests", "Endangered", "https://www.worldwildlife.org/species/mountain-gorilla"),
                entry("animals", "sea-otter", "Sea Otter", "A keystone species in kelp ecosystems.", "Sea otters control sea urchin populations, preventing kelp collapse and helping coastal biodiversity thrive. Their return often improves nearshore ecosystem resilience.", "https://images.unsplash.com/photo-1456926631375-92c8ce872def?auto=format&fit=crop&w=1400&q=80", "50% 55%", "Coastal kelp zones", "Endangered", "https://www.worldwildlife.org/species/sea-otter"),
                entry("animals", "giant-panda", "Giant Panda", "A bamboo-forest specialist and conservation symbol.", "Giant pandas support mountain forest conservation at a landscape scale because protecting their habitat also safeguards headwater forests, understory diversity, and many co-existing species.", "https://images.unsplash.com/photo-1564349683136-77e08dba1ef7?auto=format&fit=crop&w=1400&q=80", "50% 32%", "Temperate bamboo forests", "Vulnerable", "https://www.worldwildlife.org/species/giant-panda"),
                entry("animals", "manatee", "West Indian Manatee", "A gentle grazer tied to healthy seagrass.", "Manatees maintain seagrass bed dynamics through grazing and movement. Their wellbeing depends on clean warm waters, protected migration corridors, and reduced boat-strike risks.", "https://images.unsplash.com/photo-1546026423-cc4642628d2b?auto=format&fit=crop&w=1400&q=80", "50% 52%", "Coastal lagoons and estuaries", "Vulnerable", "https://www.worldwildlife.org/species/manatee"),

                entry("birds", "bald-eagle", "Bald Eagle", "A freshwater ecosystem health indicator.", "Bald eagles signal robust aquatic food chains and protected nesting habitats. Their population rebound demonstrates the value of long-term policy and habitat restoration.", "https://images.unsplash.com/photo-1611689342806-0863700ce1e4?auto=format&fit=crop&w=1400&q=80", "50% 26%", "Rivers, lakes, coasts", "Least Concern", "https://www.audubon.org/field-guide/bird/bald-eagle"),
                entry("birds", "scarlet-macaw", "Scarlet Macaw", "A colorful rainforest seed disperser.", "Scarlet macaws require mature forests and nesting cavities. Their movement patterns help distribute seeds and support forest regeneration at scale.", "https://images.unsplash.com/photo-1591198936750-16d8e15edb9e?auto=format&fit=crop&w=1400&q=80", "50% 34%", "Lowland rainforests", "Least Concern", "https://animaldiversity.org/accounts/Ara_macao/"),
                entry("birds", "barn-owl", "Barn Owl", "A nocturnal hunter tied to open farmland.", "Barn owls regulate rodent populations and depend on hedgerows, field margins, and quiet nesting spaces. Changes in land use can quickly affect local numbers.", "https://images.unsplash.com/photo-1522926193341-e9ffd686c60f?auto=format&fit=crop&w=1400&q=80", "50% 35%", "Farmland and grassland", "Least Concern", "https://www.britannica.com/animal/barn-owl"),
                entry("birds", "peregrine-falcon", "Peregrine Falcon", "A high-speed raptor found worldwide.", "Peregrine falcons are top aerial predators whose recovery highlights effective toxin regulation and nesting protection. They now adapt even to urban cliff-like structures.", "https://images.unsplash.com/photo-1452570053594-1b985d6ea890?auto=format&fit=crop&w=1400&q=80", "50% 32%", "Cliffs, coasts, cities", "Least Concern", "https://www.audubon.org/field-guide/bird/peregrine-falcon"),
                entry("birds", "greater-flamingo", "Greater Flamingo", "A wetland specialist in saline lagoons.", "Flamingos depend on productive shallow wetlands where food blooms seasonally. Water management and pollution control are key to colony stability.", "https://images.unsplash.com/photo-1452570053594-1b985d6ea890?auto=format&fit=crop&w=1400&q=80", "50% 70%", "Lagoons and estuaries", "Least Concern", "https://www.iucnredlist.org/species/22697360/93611571"),
                entry("birds", "kingfisher", "Common Kingfisher", "A river-edge bird requiring clean water.", "Kingfishers reflect stream quality and fish availability. Riparian vegetation and low pollution are essential to maintain breeding success.", "https://images.unsplash.com/photo-1501706362039-c6e80948f5fa?auto=format&fit=crop&w=1400&q=80", "50% 45%", "Rivers and wetlands", "Least Concern", "https://www.rspb.org.uk/birds-and-wildlife/kingfisher"),
                entry("birds", "hummingbird", "Hummingbird", "A pollinator with high energy demand.", "Hummingbirds connect flowering plants and pollination networks. Habitat continuity and native flowering species are central to their ecological role.", "https://images.unsplash.com/photo-1444464666168-49d633b86797?auto=format&fit=crop&w=1400&q=80", "50% 40%", "Forests and gardens", "Least Concern", "https://www.nationalgeographic.com/animals/birds/facts/hummingbirds"),
                entry("birds", "great-hornbill", "Great Hornbill", "A large-fruited tree disperser in Asian forests.", "Great hornbills transport seeds over long distances and are highly sensitive to old-growth forest loss. Their decline often mirrors canopy fragmentation.", "https://images.unsplash.com/photo-1591608971362-f08b2a75731a?auto=format&fit=crop&w=1400&q=80", "50% 28%", "Tropical forests", "Vulnerable", "https://www.iucnredlist.org/species/22682464/184778946"),
                entry("birds", "arctic-tern", "Arctic Tern", "A long-distance migrant crossing oceans.", "Arctic terns undertake one of the longest migrations on Earth, linking polar marine ecosystems. Their trends help track climate-driven changes in ocean productivity.", "https://images.unsplash.com/photo-1470115636492-6d2b56f9146d?auto=format&fit=crop&w=1400&q=80", "50% 48%", "Polar coasts and open ocean", "Least Concern", "https://www.audubon.org/field-guide/bird/arctic-tern"),
                entry("birds", "peacock", "Indian Peafowl", "A ground-foraging bird with elaborate courtship.", "Peafowl use mixed scrub and forest-edge habitats and are culturally important in many regions. Habitat pressure and disturbance affect breeding behavior.", "https://images.unsplash.com/photo-1549608276-5786777e6587?auto=format&fit=crop&w=1400&q=80", "50% 35%", "Scrub and forest edges", "Least Concern", "https://www.britannica.com/animal/peafowl"),
                entry("birds", "atlantic-puffin", "Atlantic Puffin", "A charismatic seabird tied to productive cold waters.", "Atlantic puffins rely on healthy forage fish populations and secure nesting cliffs. Their breeding success provides early warning signals of food-web shifts in marine ecosystems.", "https://images.unsplash.com/photo-1544551763-77ef2d0cfc6c?auto=format&fit=crop&w=1400&q=80", "50% 42%", "North Atlantic coasts", "Vulnerable", "https://www.audubon.org/field-guide/bird/atlantic-puffin"),
                entry("birds", "whooping-crane", "Whooping Crane", "A wetland-dependent migratory crane.", "Whooping cranes depend on intact breeding marshes and protected stopover wetlands along migration corridors. Their recovery demonstrates how coordinated habitat protection can rebuild a species.", "https://images.unsplash.com/photo-1510798831971-661eb04b3739?auto=format&fit=crop&w=1400&q=80", "50% 45%", "Marshes and coastal wetlands", "Endangered", "https://www.fws.gov/species/whooping-crane-grus-americana"),

                entry("ecosystems", "mangrove-forest", "Mangrove Forest", "Coastal nurseries and storm buffers.", "Mangroves reduce erosion, support fisheries, and store significant blue carbon. Protecting tidal flow and reducing coastal conversion are central to restoration success.", "https://images.unsplash.com/photo-1584438784894-089d6a62b8fa?auto=format&fit=crop&w=1400&q=80", "50% 58%", "Intertidal tropical coast", "High conservation priority", "https://www.iucn.org/resources/issues-brief/mangroves-and-climate-change"),
                entry("ecosystems", "coral-reef", "Coral Reef", "Biodiversity hubs of tropical seas.", "Coral reefs support fisheries, coastal protection, and tourism livelihoods. Rising heat and acidification increase bleaching and slow reef recovery.", "https://images.unsplash.com/photo-1546026423-cc4642628d2b?auto=format&fit=crop&w=1400&q=80", "50% 52%", "Warm shallow oceans", "Critically stressed", "https://www.noaa.gov/education/resource-collections/marine-life/coral-reef-ecosystems"),
                entry("ecosystems", "tropical-rainforest", "Tropical Rainforest", "A high-diversity biome and carbon sink.", "Rainforests regulate rainfall cycles, store carbon, and support unparalleled species diversity. Deforestation and fragmentation reduce resilience and increase edge effects.", "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1400&q=80", "50% 45%", "Humid equatorial zones", "High conservation priority", "https://www.worldwildlife.org/biomes/tropical-and-subtropical-moist-broadleaf-forests"),
                entry("ecosystems", "savannah", "Savannah Grassland", "Open landscapes shaped by fire and grazing.", "Savannah ecosystems rely on seasonal cycles, herbivore movement, and predator-prey balance. Fragmented movement routes can collapse regional ecological dynamics.", "https://images.unsplash.com/photo-1516426122078-c23e76319801?auto=format&fit=crop&w=1400&q=80", "50% 50%", "Tropical and subtropical grasslands", "Priority landscape", "https://www.worldwildlife.org/biomes/tropical-and-subtropical-grasslands-savannas-and-shrublands"),
                entry("ecosystems", "wetland", "Wetland", "Natural water filters and flood buffers.", "Wetlands absorb flood pulses, filter pollutants, and provide breeding grounds for birds, fish, and amphibians. Their degradation directly impacts water security.", "https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1400&q=80", "50% 60%", "Floodplains and marshes", "Threatened globally", "https://www.ramsar.org/about/wetlands"),
                entry("ecosystems", "temperate-forest", "Temperate Forest", "Seasonal forests with rich understory communities.", "Temperate forests store carbon, stabilize soil, and support pollinators and mammals through seasonal diversity. Sustainable management keeps habitat structure intact.", "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1400&q=80", "50% 35%", "Mid-latitude forests", "Moderate concern", "https://www.britannica.com/science/temperate-forest"),
                entry("ecosystems", "alpine-ecosystem", "Alpine Ecosystem", "High-elevation habitats with short growing seasons.", "Alpine ecosystems host specialized species adapted to cold and low oxygen. Warming shifts species ranges uphill and compresses available habitat.", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1400&q=80", "50% 52%", "Mountain zones", "Climate-sensitive", "https://www.unep.org/news-and-stories/story/alpine-ecosystems-under-pressure"),
                entry("ecosystems", "desert-ecosystem", "Desert Ecosystem", "Water-limited systems with specialized life.", "Deserts host resilient plants and animals adapted to heat and scarcity. Land degradation and groundwater overuse can quickly destabilize these systems.", "https://images.unsplash.com/photo-1509316785289-025f5b846b35?auto=format&fit=crop&w=1400&q=80", "50% 58%", "Arid and semi-arid regions", "High vulnerability", "https://www.britannica.com/science/desert"),
                entry("ecosystems", "seagrass-meadow", "Seagrass Meadow", "Shallow marine meadows storing blue carbon.", "Seagrass meadows stabilize sediment, support fisheries nurseries, and absorb carbon. They decline under poor water quality and physical disturbance.", "https://images.unsplash.com/photo-1544551763-7ef4200d2a1a?auto=format&fit=crop&w=1400&q=80", "50% 58%", "Coastal shallow waters", "Declining globally", "https://www.unep.org/resources/report/out-blue-seagrass-ecosystem"),
                entry("ecosystems", "boreal-forest", "Boreal Forest", "Vast northern forest and carbon reservoir.", "Boreal forests regulate global climate through carbon storage and albedo effects. Fire regime shifts and warming are major emerging pressures.", "https://images.unsplash.com/photo-1476231682828-37e571bc172f?auto=format&fit=crop&w=1400&q=80", "50% 45%", "Northern latitudes", "Climate-sensitive", "https://www.worldwildlife.org/biomes/boreal-forests-taiga"),
                entry("ecosystems", "peatland", "Peatland Ecosystem", "Waterlogged carbon stores with global climate value.", "Peatlands hold immense long-term carbon stocks and regulate watershed behavior. Drainage and peat extraction can rapidly convert these landscapes from carbon sinks into major emission sources.", "https://images.unsplash.com/photo-1477414348463-c0eb7f1359b6?auto=format&fit=crop&w=1400&q=80", "50% 56%", "Bogs, fens, and mire systems", "High conservation priority", "https://www.unep.org/explore-topics/ecosystems-and-biodiversity/what-we-do/protecting-peatlands"),
                entry("ecosystems", "river-delta", "River Delta", "Dynamic floodplain mosaics supporting fisheries and birds.", "River deltas connect upstream sediment flow, wetlands, agriculture, and coastal nurseries. Restoring flow variability and reducing pollution improves resilience for both biodiversity and communities.", "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?auto=format&fit=crop&w=1400&q=80", "50% 50%", "Estuarine floodplain complexes", "High vulnerability", "https://www.worldwildlife.org/places/greater-mekong")
            );

    private static ExploreEntryView entry(String category,
                          String slug,
                          String name,
                          String shortDescription,
                          String detail,
                          String imageUrl,
                          String imagePosition,
                          String habitat,
                          String conservationStatus,
                          String sourceUrl) {
            return ExploreEntryView.builder()
                .category(category)
                .slug(slug)
                .name(name)
                .shortDescription(shortDescription)
                .detail(detail)
                .imageUrl(imageUrl)
                .imagePosition(imagePosition)
                .habitat(habitat)
                .conservationStatus(conservationStatus)
                .sourceUrl(sourceUrl)
                .build();
            }

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

    public Map<String, List<ExploreEntryView>> getExploreEntriesByCategory() {
        return EXPLORE_ENTRIES.stream()
                .filter(entry -> !"features".equalsIgnoreCase(entry.getCategory()))
                .map(this::withResolvedImage)
                .collect(Collectors.groupingBy(ExploreEntryView::getCategory, LinkedHashMap::new, Collectors.toList()));
    }

    public Optional<ExploreEntryView> findExploreEntry(String category, String slug) {
        if (category == null || slug == null) {
            return Optional.empty();
        }
        return EXPLORE_ENTRIES.stream()
                .filter(entry -> entry.getCategory().equalsIgnoreCase(category) && entry.getSlug().equalsIgnoreCase(slug))
                .map(this::withResolvedImage)
                .findFirst();
    }

    public List<WildlifePhotoView> getWildlifePhotographyGallery() {
        List<String> gallerySubjects = List.of(
            "Jaguar",
            "Emperor penguin",
            "Blue whale",
            "Red panda",
            "Komodo dragon",
            "African wild dog",
            "Atlantic puffin",
            "Snow leopard",
            "Mountain gorilla",
            "Sea turtle",
            "Orca",
            "Great hornbill"
        );

        return gallerySubjects.stream()
            .map(this::getWikipediaSummary)
            .flatMap(Optional::stream)
            .map(summary -> WildlifePhotoView.builder()
                .title(summary.title)
                .ownerName("Wikimedia Commons")
                .location(summary.description == null || summary.description.isBlank()
                    ? "Wikipedia field archive"
                    : summary.description)
                .detail(summary.extract == null || summary.extract.isBlank()
                    ? "Field documentation from Wikimedia species archives."
                    : summary.extract)
                .imageUrl(summary.imageUrl)
                .imagePosition("50% 50%")
                .build())
                .toList();
    }

    private ExploreEntryView withResolvedImage(ExploreEntryView entry) {
        String resolvedImage = resolveWikipediaImage(entry).orElse(entry.getImageUrl());
        return ExploreEntryView.builder()
                .category(entry.getCategory())
                .slug(entry.getSlug())
                .name(entry.getName())
                .shortDescription(entry.getShortDescription())
                .detail(entry.getDetail())
                .imageUrl(resolvedImage)
                .imagePosition(entry.getImagePosition())
                .habitat(entry.getHabitat())
                .conservationStatus(entry.getConservationStatus())
                .sourceUrl(entry.getSourceUrl())
                .build();
    }

    private Optional<String> resolveWikipediaImage(ExploreEntryView entry) {
        String title = switch (entry.getSlug()) {
            case "polar-bear" -> "Polar bear";
            case "mangrove-forest" -> "Mangrove";
            case "savannah" -> "Savanna";
            case "temperate-forest" -> "Temperate forest";
            case "seagrass-meadow" -> "Seagrass meadow";
            case "boreal-forest" -> "Taiga";
            case "peatland" -> "Peatland";
            case "river-delta" -> "River delta";
            case "gray-wolf" -> "Wolf";
            case "manatee" -> "West Indian manatee";
            case "giant-panda" -> "Giant panda";
            case "mountain-gorilla" -> "Mountain gorilla";
            case "greater-flamingo" -> "Greater flamingo";
            case "atlantic-puffin" -> "Atlantic puffin";
            case "whooping-crane" -> "Whooping crane";
            case "indian-peafowl", "peacock" -> "Indian peafowl";
            default -> entry.getName();
        };

        String cached = wikiImageCache.get(title);
        if (cached != null) {
            return Optional.of(cached);
        }

        return getWikipediaSummary(title).map(summary -> {
            wikiImageCache.put(title, summary.imageUrl);
            return summary.imageUrl;
        });
    }

    private Optional<WikiSummary> getWikipediaSummary(String title) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20");
            String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encodedTitle;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "WildBeyond/1.0 (ExploreImageResolver)")
                    .timeout(Duration.ofSeconds(Math.max(5, requestTimeoutSeconds)))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String image = root.path("originalimage").path("source").textValue();
            if (image == null || image.isBlank()) {
                image = root.path("thumbnail").path("source").textValue();
            }
            if (image == null || image.isBlank()) {
                return Optional.empty();
            }

            String summaryTitle = root.path("title").textValue();
            String description = root.path("description").textValue();
            String extract = root.path("extract").textValue();
            return Optional.of(new WikiSummary(
                    summaryTitle == null || summaryTitle.isBlank() ? title : summaryTitle,
                    description,
                    extract,
                    image
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private record WikiSummary(String title, String description, String extract, String imageUrl) {
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
