package org.assessment.security;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
                "X-User-Id, user-42",
                "X-User-Role, INSTRUCTOR"
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

        @ParameterizedTest(name = "{index} => header={0}, value={1}")
        @MethodSource("missingOrBlankHeaders")
        @DisplayName("should not add header when it is missing or blank")
        void shouldNotAddHeaderWhenMissingOrBlank(String headerName, String headerValue) {

            MockHttpServletRequest request = bindRequest();

            if (headerValue != null) {
                request.addHeader(headerName, headerValue);
            }

            RequestTemplate template = new RequestTemplate();
            interceptor.apply(template);

            assertThat(template.headers().get(headerName)).isNullOrEmpty();
        }

        static Stream<Arguments> missingOrBlankHeaders() {
            return Stream.of(
                    arguments("Authorization", null),
                    arguments("Authorization", "   "),
                    arguments("X-User-Id", "   "),
                    arguments("X-User-Role", "   "),
                    arguments("X-User-Role", null)
            );
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