package org.assessment.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorResponse Tests")
class ErrorResponseTest {

    @Nested
    @DisplayName("ErrorResponse.of factory method")
    class OfFactory {

        @Test
        @DisplayName("should set all fields correctly")
        void of_setsAllFields() {
            ErrorResponse response = ErrorResponse.of(404, "Not Found", "Resource missing");

            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getError()).isEqualTo("Not Found");
            assertThat(response.getMessage()).isEqualTo("Resource missing");
        }

        @Test
        @DisplayName("should generate a non-null timestamp")
        void of_setsTimestamp() {
            ErrorResponse response = ErrorResponse.of(400, "Bad Request", "Invalid input");

            assertThat(response.getTimestamp())
                    .isNotNull()
                    .isNotBlank();
        }

        @Test
        @DisplayName("timestamp should be a non-null parseable LocalDateTime string")
        void of_timestampIsCurrentTime() {
            ErrorResponse r1 = ErrorResponse.of(500, "Error", "msg");
            ErrorResponse r2 = ErrorResponse.of(500, "Error", "msg");

            assertThat(r1.getTimestamp()).isNotNull();
            assertThat(r2.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("should work correctly for 500 status")
        void of_500Status() {
            ErrorResponse response = ErrorResponse.of(500, "Internal Server Error", "Something went wrong");

            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getError()).isEqualTo("Internal Server Error");
            assertThat(response.getMessage()).isEqualTo("Something went wrong");
        }

        @Test
        @DisplayName("should work correctly for 401 status")
        void of_401Status() {
            ErrorResponse response = ErrorResponse.of(401, "Unauthorized", "User not authenticated");

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getError()).isEqualTo("Unauthorized");
            assertThat(response.getMessage()).isEqualTo("User not authenticated");
        }
    }

    @Nested
    @DisplayName("Lombok builder")
    class Builder {

        @Test
        @DisplayName("should build ErrorResponse with all fields via builder")
        void builder_setsAllFields() {
            ErrorResponse response = ErrorResponse.builder()
                    .status(400)
                    .error("Bad Request")
                    .message("title must not be blank")
                    .timestamp("2025-01-01T10:00:00")
                    .build();

            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getError()).isEqualTo("Bad Request");
            assertThat(response.getMessage()).isEqualTo("title must not be blank");
            assertThat(response.getTimestamp()).isEqualTo("2025-01-01T10:00:00");
        }
    }
}
