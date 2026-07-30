package org.assessment.storage;

import org.assessment.exception.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3ServiceImpl Tests")
class S3ServiceImplTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3ServiceImpl s3Service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "region", "ap-south-1");
    }

    // -------------------------------------------------------------------------
    // uploadFile(MultipartFile)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("uploadFile(file)")
    class UploadFileNoFolder {

        @Test
        @DisplayName("should return URL with no folder prefix when folder is empty")
        void upload_noFolder_returnsUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", "pdf bytes".getBytes());

            String url = s3Service.uploadFile(file);

            assertThat(url)
                    .startsWith("https://test-bucket.s3.ap-south-1.amazonaws.com/")
                    .endsWith(".pdf");
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("should return null when file is null")
        void upload_nullFile_returnsNull() {
            String url = s3Service.uploadFile(null);

            assertThat(url).isNull();
            verifyNoInteractions(s3Client);
        }

        @Test
        @DisplayName("should return null when file is empty")
        void upload_emptyFile_returnsNull() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", new byte[0]);

            String url = s3Service.uploadFile(emptyFile);

            assertThat(url).isNull();
            verifyNoInteractions(s3Client);
        }
    }

    // -------------------------------------------------------------------------
    // uploadFile(MultipartFile, String folder)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("uploadFile(file, folder)")
    class UploadFileWithFolder {

        @Test
        @DisplayName("should include folder prefix in S3 key")
        void upload_withFolder_keyIncludesFolder() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "assignment.pdf", "application/pdf", "content".getBytes());

            ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);

            s3Service.uploadFile(file, "assignments");

            verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
            PutObjectRequest request = captor.getValue();
            assertThat(request.key())
                    .startsWith("assignments/")
                    .endsWith(".pdf");
            assertThat(request.bucket()).isEqualTo("test-bucket");
        }

        @Test
        @DisplayName("should use filename extension in generated key")
        void upload_preservesFileExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "solution.zip", "application/zip", "zip content".getBytes());

            ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
            s3Service.uploadFile(file, "submissions");

            verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
            assertThat(captor.getValue().key()).endsWith(".zip");
        }

        @Test
        @DisplayName("should handle file with no extension gracefully")
        void upload_noExtension_keyHasNoExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "noextfile", "text/plain", "text".getBytes());

            ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
            s3Service.uploadFile(file, "misc");

            verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
            assertThat(captor.getValue().key()).startsWith("misc/");
        }

        @Test
        @DisplayName("should return full S3 HTTPS URL after upload")
        void upload_returnsFullUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "bytes".getBytes());

            String url = s3Service.uploadFile(file, "assignments");

            assertThat(url).startsWith("https://test-bucket.s3.ap-south-1.amazonaws.com/assignments/");
        }

        @Test
        @DisplayName("should throw FileUploadException when S3 client throws IOException")
        void upload_ioException_throwsFileUploadException() {
            MockMultipartFile badFile = new MockMultipartFile(
                    "file", "bad.pdf", "application/pdf", "bytes".getBytes()) {
                @Override
                public byte[] getBytes() throws java.io.IOException {
                    throw new java.io.IOException("disk read error");
                }
            };

            assertThatThrownBy(() -> s3Service.uploadFile(badFile, "assignments"))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("Failed to upload file to S3");
        }

        @Test
        @DisplayName("should return null when file is null")
        void upload_nullFile_returnsNull() {
            String url = s3Service.uploadFile(null, "assignments");
            assertThat(url).isNull();
            verifyNoInteractions(s3Client);
        }
    }

    // -------------------------------------------------------------------------
    // deleteFile
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("deleteFile")
    class DeleteFile {

        @Test
        @DisplayName("should call S3 deleteObject with extracted key from full URL")
        void delete_fullUrl_extractsKey() {
            String url = "https://test-bucket.s3.ap-south-1.amazonaws.com/assignments/uuid-file.pdf";

            ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

            s3Service.deleteFile(url);

            verify(s3Client).deleteObject(captor.capture());
            assertThat(captor.getValue().key()).isEqualTo("assignments/uuid-file.pdf");
            assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        }

        @Test
        @DisplayName("should use fileKey directly when it is not an HTTP URL")
        void delete_plainKey_usedDirectly() {
            String key = "submissions/uuid-file.pdf";

            ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            s3Service.deleteFile(key);

            verify(s3Client).deleteObject(captor.capture());
            assertThat(captor.getValue().key()).isEqualTo("submissions/uuid-file.pdf");
        }

        @Test
        @DisplayName("should do nothing when fileKey is null")
        void delete_nullKey_noInteraction() {
            s3Service.deleteFile(null);
            verifyNoInteractions(s3Client);
        }

        @Test
        @DisplayName("should do nothing when fileKey is blank")
        void delete_blankKey_noInteraction() {
            s3Service.deleteFile("   ");
            verifyNoInteractions(s3Client);
        }

        @Test
        @DisplayName("should not throw when S3 client throws during delete")
        void delete_s3Throws_doesNotPropagate() {
            doThrow(new RuntimeException("S3 error"))
                    .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

            s3Service.deleteFile("assignments/some-file.pdf");
            // no exception expected — silently logs and continues
        }
    }

    // -------------------------------------------------------------------------
    // getFileUrl
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getFileUrl")
    class GetFileUrl {

        @Test
        @DisplayName("should build full S3 URL from a plain key")
        void getFileUrl_plainKey_buildsUrl() {
            String url = s3Service.getFileUrl("assignments/uuid.pdf");

            assertThat(url).isEqualTo(
                    "https://test-bucket.s3.ap-south-1.amazonaws.com/assignments/uuid.pdf");
        }

        @Test
        @DisplayName("should return the URL unchanged when it already starts with https://")
        void getFileUrl_alreadyHttps_returnsAsIs() {
            String existing = "https://test-bucket.s3.ap-south-1.amazonaws.com/assignments/uuid.pdf";

            String url = s3Service.getFileUrl(existing);

            assertThat(url).isEqualTo(existing);
        }

        @Test
        @DisplayName("should return the URL unchanged when it starts with http://")
        void getFileUrl_alreadyHttp_returnsAsIs() {
            String existing = "http://some-cdn.com/file.pdf";

            String url = s3Service.getFileUrl(existing);

            assertThat(url).isEqualTo(existing);
        }

        @Test
        @DisplayName("should return null when fileKey is null")
        void getFileUrl_nullKey_returnsNull() {
            assertThat(s3Service.getFileUrl(null)).isNull();
        }

        @Test
        @DisplayName("should return null when fileKey is blank")
        void getFileUrl_blankKey_returnsNull() {
            assertThat(s3Service.getFileUrl("   ")).isNull();
        }

        @Test
        @DisplayName("getFileUrl with folder-prefixed key should include folder in URL")
        void getFileUrl_withFolderKey_includesFolderInUrl() {
            String url = s3Service.getFileUrl("submissions/my-file.pdf");

            assertThat(url).isEqualTo(
                    "https://test-bucket.s3.ap-south-1.amazonaws.com/submissions/my-file.pdf");
        }
    }
}
