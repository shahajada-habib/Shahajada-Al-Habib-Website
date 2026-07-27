package com.blogcms;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.blogcms.category.Category;
import com.blogcms.category.CategoryRepository;
import com.blogcms.comment.CommentRepository;
import com.blogcms.media.MediaAssetRepository;
import com.blogcms.news.NewsRepository;
import com.blogcms.reaction.ReactionRepository;
import com.blogcms.tag.TagRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:blog_cms_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "app.seed.default-users.enabled=true",
        "app.seed.default-password=1234",
        "app.jwt.secret=test-secret-for-news-security-integration-tests",
        "app.jwt.expiration-seconds=3600",
        "app.media.upload-dir=target/test-uploads",
        "app.rate-limit.login-per-minute=10000"
})
@AutoConfigureMockMvc
class NewsSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        reactionRepository.deleteAll();
        commentRepository.deleteAll();
        newsRepository.deleteAll();
        tagRepository.deleteAll();
        mediaAssetRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = new Category();
        category.setName("National");
        category.setSlug("national");
        category.setStatus("active");
        categoryRepository.save(category);
    }

    @Test
    void loginAdminEditorReporterAndGetJwt() throws Exception {
        assertToken(login("admin"));
        assertToken(login("editor"));
        assertToken(login("reporter"));
    }

    @Test
    void authLoginPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("content-type")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void publicActiveCategoriesEndpointIsOpen() throws Exception {
        Category inactiveCategory = new Category();
        inactiveCategory.setName("Hidden");
        inactiveCategory.setSlug("hidden");
        inactiveCategory.setStatus("inactive");
        categoryRepository.save(inactiveCategory);

        mockMvc.perform(get("/api/public/categories/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("National"))
                .andExpect(jsonPath("$[0].status").value("active"));
    }

    @Test
    void publicNewsEndpointOnlyReturnsPublishedNewsDueNow() throws Exception {
        String adminToken = login("admin");
        Map<String, Object> duePayload = newsPayload("Due published story", "due-published-story", "published");
        duePayload.put("publishDate", "2026-01-01T10:00");
        Map<String, Object> futurePayload = newsPayload("Future story", "future-story", "published");
        futurePayload.put("publishDate", "2099-01-01T10:00");

        mockMvc.perform(post("/api/news")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(duePayload)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/news")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(futurePayload)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/news")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("due-published-story"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist());
    }

    @Test
    void adminPaginatedNewsUsesStablePageResponse() throws Exception {
        String adminToken = login("admin");
        createNews(adminToken, "Paged admin story", "paged-admin-story", "draft");

        mockMvc.perform(get("/api/news")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("paged-admin-story"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist());
    }

    @Test
    void publicNewsSearchReturnsOnlyDuePublishedPageResponse() throws Exception {
        String adminToken = login("admin");
        Map<String, Object> duePayload = newsPayload("Climate river update", "climate-river-update", "published");
        duePayload.put("subtitle", "Flood watch");
        duePayload.put("content", "<p>River protection plan for the north.</p>");
        duePayload.put("publishDate", "2026-01-01T10:00");
        Map<String, Object> futurePayload = newsPayload("River future embargo", "river-future-embargo", "published");
        futurePayload.put("content", "<p>River article that should not appear yet.</p>");
        futurePayload.put("publishDate", "2099-01-01T10:00");
        Map<String, Object> draftPayload = newsPayload("River draft", "river-draft", "draft");
        draftPayload.put("content", "<p>River draft should not appear publicly.</p>");

        mockMvc.perform(post("/api/news")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(duePayload)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/news")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(futurePayload)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/news")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(draftPayload)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/news/search")
                        .param("q", "river")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("climate-river-update"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist());
    }

    @Test
    void tagsAreStoredAsEntitiesAndPublishedTagsAreExposed() throws Exception {
        String adminToken = login("admin");
        Map<String, Object> payload = newsPayload("Tagged story", "tagged-story", "published");
        payload.put("tagNames", List.of("Election", "City Desk", "Budget"));

        mockMvc.perform(post("/api/news")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagNames.length()").value(3))
                .andExpect(jsonPath("$.tagNames[0]").value("Budget"));

        mockMvc.perform(get("/api/public/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItems("Election", "City Desk", "Budget")));

        mockMvc.perform(get("/api/admin/tags")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].articleCount").exists());
    }

    @Test
    void adminUserManagementDoesNotExposePasswordHash() throws Exception {
        String adminToken = login("admin");

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "desk-user",
                                "password", "secret123",
                                "role", "editor",
                                "status", "active"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("desk-user"))
                .andExpect(jsonPath("$.role").value("editor"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void userValidationRejectsWeakPasswordAndUnsafeUsername() throws Exception {
        String adminToken = login("admin");

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "bad username",
                                "password", "secret123",
                                "role", "editor",
                                "status", "active"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "shortpass",
                                "password", "1234",
                                "role", "editor",
                                "status", "active"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void articleHtmlIsSanitizedBeforeSaving() throws Exception {
        String adminToken = login("admin");
        Map<String, Object> payload = newsPayload("Unsafe article", "unsafe-article", "published");
        payload.put("content", "<p>Safe</p><script>alert('xss')</script><img src=\"javascript:alert(1)\">");

        mockMvc.perform(post("/api/news")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(containsString("<script"))))
                .andExpect(jsonPath("$.content", not(containsString("javascript:"))))
                .andExpect(jsonPath("$.content", containsString("Safe")));
    }

    @Test
    void commentsStayPendingUntilModerated() throws Exception {
        String adminToken = login("admin");
        long newsId = createNews(adminToken, "Commented story", "commented-story", "published");

        MvcResult commentResult = mockMvc.perform(post("/api/public/news/{newsId}/comments", newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("author", "Reader", "content", "Useful report"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"))
                .andReturn();

        long commentId = objectMapper.readTree(commentResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/public/news/{newsId}/comments", newsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/admin/comments")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("pending"));

        mockMvc.perform(patch("/api/admin/comments/{id}/approve", commentId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));

        mockMvc.perform(get("/api/public/news/{newsId}/comments", newsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Useful report"));
    }

    @Test
    void publicReactionIsIpBasedCanChangeAndPersistsInArticleResponse() throws Exception {
        String adminToken = login("admin");
        long newsId = createNews(adminToken, "Reacted story", "reacted-story", "published");

        mockMvc.perform(post("/api/public/news/{newsId}/reaction", newsId)
                        .with((request) -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reactionType", "like"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.dislikeCount").value(0))
                .andExpect(jsonPath("$.reactionType").value("like"));

        mockMvc.perform(post("/api/public/news/{newsId}/reaction", newsId)
                        .with((request) -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reactionType", "like"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.dislikeCount").value(0));

        mockMvc.perform(post("/api/public/news/{newsId}/reaction", newsId)
                        .with((request) -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reactionType", "dislike"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.dislikeCount").value(1))
                .andExpect(jsonPath("$.reactionType").value("dislike"));

        mockMvc.perform(get("/api/public/news/reacted-story"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.dislikeCount").value(1));
    }

    @Test
    void publicCommentInputIsPlainTextSanitized() throws Exception {
        String adminToken = login("admin");
        long newsId = createNews(adminToken, "Sanitized comment story", "sanitized-comment-story", "published");

        MvcResult commentResult = mockMvc.perform(post("/api/public/news/{newsId}/comments", newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("author", "<b>Reader</b>", "content", "<script>alert(1)</script>Hello"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("Reader"))
                .andExpect(jsonPath("$.content", not(containsString("<script"))))
                .andExpect(jsonPath("$.content").value("Hello"))
                .andReturn();

        long commentId = objectMapper.readTree(commentResult.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(patch("/api/admin/comments/{id}/approve", commentId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void reporterCanUploadMediaButCannotDelete() throws Exception {
        String reporterToken = login("reporter");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake image bytes".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("title", "Reporter photo")
                        .header("Authorization", bearer(reporterToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("Reporter photo"))
                .andExpect(jsonPath("$.fileUrl", containsString("/uploads/")))
                .andExpect(jsonPath("$.contentType").value(MediaType.IMAGE_PNG_VALUE))
                .andExpect(jsonPath("$.uploadedBy").value("reporter"))
                .andReturn();

        long mediaId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/media/{id}", mediaId)
                        .header("Authorization", bearer(reporterToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void uploadRejectsUnsupportedFileType() throws Exception {
        String reporterToken = login("reporter");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.svg",
                "image/svg+xml",
                "<svg><script>alert(1)</script></svg>".getBytes());

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("title", "Bad upload")
                        .header("Authorization", bearer(reporterToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("only jpg, png, webp, or gif images are allowed"));
    }

    @Test
    void editorCanListAndDeleteMedia() throws Exception {
        String editorToken = login("editor");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "desk.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image bytes".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("title", "Desk image")
                        .header("Authorization", bearer(editorToken)))
                .andExpect(status().isOk())
                .andReturn();

        long mediaId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/media")
                        .header("Authorization", bearer(editorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(mediaId))
                .andExpect(jsonPath("$[0].fileUrl", containsString("/uploads/")));

        mockMvc.perform(delete("/api/media/{id}", mediaId)
                        .header("Authorization", bearer(editorToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanCreateUpdateDeleteNews() throws Exception {
        String adminToken = login("admin");
        long newsId = createNews(adminToken, "Admin story", "admin-story", "draft");

        mockMvc.perform(put("/api/news/{id}", newsId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newsPayload("Admin story updated", "admin-story", "review"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin story updated"))
                .andExpect(jsonPath("$.status").value("review"));

        mockMvc.perform(delete("/api/news/{id}", newsId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void editorCanCreateUpdatePublishButCannotDelete() throws Exception {
        String editorToken = login("editor");
        long newsId = createNews(editorToken, "Editor story", "editor-story", "draft");

        mockMvc.perform(put("/api/news/{id}", newsId)
                        .header("Authorization", bearer(editorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newsPayload("Editor story updated", "editor-story", "review"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("review"));

        mockMvc.perform(patch("/api/news/{id}/status", newsId)
                        .header("Authorization", bearer(editorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "published"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"));

        mockMvc.perform(delete("/api/news/{id}", newsId)
                        .header("Authorization", bearer(editorToken)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("admin required to delete news"))
                .andExpect(jsonPath("$.path").value("/api/news/" + newsId))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void reporterCanCreateOwnDraftAndReviewButCannotPublish() throws Exception {
        String reporterToken = login("reporter");

        createNews(reporterToken, "Reporter draft", "reporter-draft", "draft");
        long reviewId = createNews(reporterToken, "Reporter review", "reporter-review", "review");

        mockMvc.perform(patch("/api/news/{id}/status", reviewId)
                        .header("Authorization", bearer(reporterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "published"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void reporterCannotEditAnotherReportersNews() throws Exception {
        String reporterToken = login("reporter");
        String adminToken = login("admin");
        long adminNewsId = createNews(adminToken, "Admin owned story", "admin-owned-story", "draft");

        mockMvc.perform(put("/api/news/{id}", adminNewsId)
                        .header("Authorization", bearer(reporterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newsPayload("Reporter edit attempt", "admin-owned-story", "draft"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("reporter can edit only own news"));
    }

    @Test
    void duplicateSlugReturns400() throws Exception {
        String adminToken = login("admin");
        createNews(adminToken, "Original slug", "duplicate-slug", "draft");

        mockMvc.perform(post("/api/news")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newsPayload("Duplicate slug", "duplicate-slug", "draft"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("slug already exists"))
                .andExpect(jsonPath("$.path").value("/api/news"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void missingCategoryReturns400() throws Exception {
        String adminToken = login("admin");
        Map<String, Object> payload = newsPayload("Missing category", "missing-category", "draft");
        payload.put("category", "does-not-exist");

        mockMvc.perform(post("/api/news")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("category does not exist"));
    }

    @Test
    void unauthenticatedProtectedRequestReturnsJson401() throws Exception {
        mockMvc.perform(get("/api/news"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/news"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void forbiddenRequestReturnsJson403() throws Exception {
        String editorToken = login("editor");
        long newsId = createNews(editorToken, "Forbidden delete", "forbidden-delete", "draft");

        mockMvc.perform(delete("/api/news/{id}", newsId)
                        .header("Authorization", bearer(editorToken)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("admin required to delete news"))
                .andExpect(jsonPath("$.path").value("/api/news/" + newsId))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private long createNews(String token, String title, String slug, String status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/news")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newsPayload(title, slug, status))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.category").value("National"))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value(username))
                .andExpect(jsonPath("$.token").value(not(nullValue())))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private void assertToken(String token) throws Exception {
        JsonNode tokenNode = objectMapper.readTree(json(Map.of("token", token))).get("token");
        org.assertj.core.api.Assertions.assertThat(tokenNode.asText()).isNotBlank();
    }

    private Map<String, Object> newsPayload(String title, String slug, String status) {
        return new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("title", title),
                Map.entry("subtitle", "Short subtitle"),
                Map.entry("content", "<p>Article body</p>"),
                Map.entry("imageUrl", "https://example.com/image.jpg"),
                Map.entry("status", status),
                Map.entry("category", "national"),
                Map.entry("reporterName", "Reporter"),
                Map.entry("source", "Desk"),
                Map.entry("tags", "tag-one,tag-two"),
                Map.entry("seoTitle", title),
                Map.entry("seoDescription", "SEO description"),
                Map.entry("slug", slug),
                Map.entry("breaking", false),
                Map.entry("featured", false),
                Map.entry("scheduledAt", ""),
                Map.entry("publishDate", "")
        ));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
