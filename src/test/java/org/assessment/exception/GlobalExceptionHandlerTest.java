package org.assessment.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // -------------------------------------------------------------------------
    // ResourceNotFoundException → 404
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("handleResourceNotFound")
    class HandleResourceNotFound {

        @Test
        @DisplayName("should return 404 with Not Found error body")
        void returns404() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Assignment not found with id: 123");

            ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody())
                    .isNotNull()
                    .satisfies(body -> {
                        assertThat(body.getStatus()).isEqualTo(404);
                        assertThat(body.getError()).isEqualTo("Not Found");
                        assertThat(body.getMessage()).isEqualTo("Assignment not found with id: 123");
                    });
        }

        @Test
        @DisplayName("should include a non-null timestamp in the response body")
        void includesTimestamp() {
            ResponseEntity<ErrorResponse> response =
                    handler.handleResourceNotFound(new ResourceNotFoundException("not found"));

            assertThat(response.getBody().getTimestamp()).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // ValidationException → 400
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("handleValidation")
    class HandleValidation {

        @Test
        @DisplayName("should return 400 with Bad Request error body")
        void returns400() {
            ValidationException ex = new ValidationException("Course ID must not be empty");

            ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Bad Request");
            assertThat(response.getBody().getMessage()).isEqualTo("Course ID must not be empty");
        }
    }

    // -------------------------------------------------------------------------
    // FileUploadException → 500
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("handleFileUpload")
    class HandleFileUpload {

        @Test
        @DisplayName("should return 500 with File Upload Error body")
        void returns500() {
            FileUploadException ex = new FileUploadException("Failed to upload file to S3: IO error");

            ResponseEntity<ErrorResponse> response = handler.handleFileUpload(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getError()).isEqualTo("File Upload Error");
            assertThat(response.getBody().getMessage()).isEqualTo("Failed to upload file to S3: IO error");
        }
    }

    // -------------------------------------------------------------------------
    // MethodArgumentNotValidException → 400
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("handleMethodArgumentNotValid")
    class HandleMethodArgumentNotValid {

        @Test
        @DisplayName("should return 400 with joined field error messages")
        void returns400_withFieldErrors() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError fieldError1 = new FieldError("obj", "title", "must not be blank");
            FieldError fieldError2 = new FieldError("obj", "courseId", "must not be blank");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
            assertThat(response.getBody().getMessage()).contains("must not be blank");
        }

        @Test
        @DisplayName("should join multiple field errors with comma separator")
        void joinsMultipleErrors() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError e1 = new FieldError("obj", "title", "Title required");
            FieldError e2 = new FieldError("obj", "passMarks", "PassMarks required");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(e1, e2));

            ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(ex);

            String msg = response.getBody().getMessage();
            assertThat(msg)
                    .contains("Title required")
                    .contains("PassMarks required")
                    .contains(",");
        }
    }

    // -------------------------------------------------------------------------
    // IllegalStateException → 401
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("handleIllegalState")
    class HandleIllegalState {

        @Test
        @DisplayName("should return 401 with Unauthorized body")
        void returns401() {
            IllegalStateException ex = new IllegalStateException("User ID not found in request headers");

            ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().getStatus()).isEqualTo(401);
            assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
            assertThat(response.getBody().getMessage()).isEqualTo("User ID not found in request headers");
        }
    }

    // -------------------------------------------------------------------------
    // Generic Exception → 500
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("handleGeneral")
    class HandleGeneral {

        @Test
        @DisplayName("should return 500 with generic message for unexpected exceptions")
        void returns500_forGenericException() {
            Exception ex = new RuntimeException("Something catastrophic happened");

            ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong");
        }

        @Test
        @DisplayName("should not leak internal exception message to client")
        void doesNotLeakExceptionMessage() {
            Exception ex = new RuntimeException("db password is abc123");

            ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex);

            assertThat(response.getBody().getMessage()).doesNotContain("abc123");
        }
    }
}
