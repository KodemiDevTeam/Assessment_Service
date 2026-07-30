package org.assessment.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileUploadResponse Tests")
class FileUploadResponseTest {

    /** Minimal concrete implementation used only for testing the interface contract. */
    private static class TestFileUploadResponse implements FileUploadResponse {
    }

    @Test
    @DisplayName("concrete implementation should be instantiable")
    void canInstantiate() {
        FileUploadResponse response = new TestFileUploadResponse();
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("two instances should not be same object")
    void twoInstancesAreDistinct() {
        FileUploadResponse r1 = new TestFileUploadResponse();
        FileUploadResponse r2 = new TestFileUploadResponse();
        assertThat(r1).isNotSameAs(r2);
    }
}
