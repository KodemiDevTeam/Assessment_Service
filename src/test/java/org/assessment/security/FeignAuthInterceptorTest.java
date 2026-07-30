package org.assessment.security;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeignAuthInterceptor Tests")
class FeignAuthInterceptorTest {

    private FeignAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new FeignAuthInterceptor();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private MockHttpServletRequest bindRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    // -------------------------------------------------------------------------
    // apply — header forwarding
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("apply — header forwarding")
    class Apply {

        @ParameterizedTest(name = "forwards {0} header with value {1}")
        @CsvSource({
                "Authorization, Bearer eyJhbGciOiJIUzI1NiJ9.test.sig",
                "X-User-Id,     user-42",
                "X-User-Role,   INSTRUCTOR"
        })
        @DisplayName("should forward each auth header to the Feign template")
        void forwardsHeader(String headerName, String headerValue) {
            MockHttpServletRequest request = bindRequest();
            request.addHeader(headerName.trim(), headerValue.trim());

            RequestTemplate template = new RequestTemplate();
            interceptor.apply(template);

            assertThat(template.headers().get(headerName.trim()))
                    .isNotNull()
                    .contains(headerValue.trim());
        }

        @Test
        @DisplayName("should forward all three headers when all are present")
        void forwardsAllThreeHeaders() {
            MockHttpServletRequest request = bindRequest();
            request.addHeader("Authorization", "Bearer token123");
            request.addHeader("X-User-Id", "user-99");
            request.addHeader("X-User-Role", "ADMIN");

            RequestTemplate template = new RequestTemplate();
            interceptor.apply(template);

            assertThat(template.headers().get("Authorization")).contains("Bearer token123");
            assertThat(template.headers().get("X-User-Id")).contains("user-99");
            assertThat(template.headers().get("X-User-Role")).contains("ADMIN");
        }

        @Test
        @DisplayName("should not add Authorization header when it is absent")
        void doesNotAddAuthHeader_whenAbsent() {
            MockHttpServletRequest request = bindRequest();
            request.addHeader("X-User-Id", "user-42");

            RequestTemplate template = new RequestTemplate();
            interceptor.apply(template);

            assertThat(template.headers().get("Authorization")).isNullOrEmpty();
        }

        @Test
        @DisplayName("should not add X-User-Id header when it is blank")
        void doesNotAddUserIdHeader_whenBlank() {
            MockHttpServletRequest request = bindRequest();
            request.addHeader("X-User-Id", "   ");

            RequestTemplate template = new RequestTemplate();
            interceptor.apply(template);

            assertThat(template.headers().get("X-User-Id")).isNullOrEmpty();
        }

        @Test
        @DisplayName("should not add Authorization header when it is blank")
        void doesNotAddAuthHeader_whenBlank() {
            MockHttpServletRequest request = bindRequest();
            request.addHeader("Authorization", "   ");

            RequestTemplate template = new RequestTemplate();
            interceptor.apply(template);

            assertThat(template.headers().get("Authorization")).isNullOrEmpty();
        }

        @Test
        @DisplayName("should not add X-User-Role header when it is blank")
        void doesNotAddUserRoleHeader_whenBlank() {
            MockHttpServletRequest request = bindRequest();
            request.addHeader("X-User-Role", "   ");

            RequestTemplate template = new RequestTemplate();
            interceptor.apply(template);

            assertThat(template.headers().get("X-User-Role")).isNullOrEmpty();
        }

        @Test
        @DisplayName("should not add X-User-Role header when it is absent")
        void doesNotAddRoleHeader_whenAbsent() {
            bindRequest();

            RequestTemplate template = new RequestTemplate();
            interceptor.apply(template);

            assertThat(template.headers().get("X-User-Role")).isNullOrEmpty();
        }

        @Test
        @DisplayName("should do nothing when no request context is bound")
        void doesNothing_whenNoRequestContext() {
            RequestContextHolder.resetRequestAttributes();

            RequestTemplate template = new RequestTemplate();
            interceptor.apply(template);

            assertThat(template.headers()).isEmpty();
        }
    }
}
