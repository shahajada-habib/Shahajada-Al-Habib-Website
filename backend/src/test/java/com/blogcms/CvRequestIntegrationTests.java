package com.blogcms;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.blogcms.cvrequest.CvRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cv_request_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "app.seed.default-users.enabled=true",
        "app.seed.default-password=1234",
        "app.jwt.secret=test-secret-for-cv-request-integration-tests",
        "app.jwt.expiration-seconds=3600",
        "app.rate-limit.login-per-minute=10000"
})
@AutoConfigureMockMvc
class CvRequestIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CvRequestRepository cvRequestRepository;

    @BeforeEach
    void setUp() {
        cvRequestRepository.deleteAll();
    }

    @Test
    void cvPdfIsNoLongerServedAsAStaticAsset() throws Exception {
        mockMvc.perform(get("/assets/cv.pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicCanSubmitCvRequestAndItIsPersistedAsPending() throws Exception {
        mockMvc.perform(post("/about/cv-request")
                        .param("name", "Recruiter Rahim")
                        .param("email", "rahim@example.com")
                        .param("purpose", "Hiring for a content role"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/about#cv"));

        var all = cvRequestRepository.findAllByOrderByCreatedAtDesc();
        org.assertj.core.api.Assertions.assertThat(all).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(all.get(0).getStatus()).isEqualTo("pending");
        org.assertj.core.api.Assertions.assertThat(all.get(0).getEmail()).isEqualTo("rahim@example.com");
    }

    @Test
    void honeypotSubmissionsAreDroppedSilently() throws Exception {
        mockMvc.perform(post("/about/cv-request")
                        .param("name", "Spammer")
                        .param("email", "spam@example.com")
                        .param("purpose", "buy cheap things")
                        .param("website", "http://spam.example"))
                .andExpect(status().is3xxRedirection());

        org.assertj.core.api.Assertions.assertThat(cvRequestRepository.count()).isZero();
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        mockMvc.perform(post("/about/cv-request")
                        .param("name", "No Email")
                        .param("email", "not-an-email")
                        .param("purpose", "test"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCvRequestEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/admin/cv-requests"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/cv-requests/file"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListUpdateStatusAndDownloadCvFile() throws Exception {
        String adminToken = login("admin");

        mockMvc.perform(post("/about/cv-request")
                        .param("name", "Karim")
                        .param("email", "karim@example.com")
                        .param("purpose", "Freelance project"))
                .andExpect(status().is3xxRedirection());

        MvcResult listResult = mockMvc.perform(get("/api/admin/cv-requests")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("pending"))
                .andExpect(jsonPath("$[0].email").value("karim@example.com"))
                .andReturn();

        long id = objectMapper.readTree(listResult.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(patch("/api/admin/cv-requests/{id}/status", id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "sent"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"))
                .andExpect(jsonPath("$.handledAt").value(not(nullValue())));

        mockMvc.perform(patch("/api/admin/cv-requests/{id}/status", id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "bogus"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/cv-requests/file")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("cv.pdf")))
                .andExpect(header().string("Content-Type", containsString("application/pdf")));
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(nullValue())))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
