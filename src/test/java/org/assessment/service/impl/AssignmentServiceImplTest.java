package org.assessment.service.impl;

import org.assessment.client.CourseServiceClient;
import org.assessment.dto.request.CreateAssignmentRequest;
import org.assessment.dto.request.UpdateAssignmentRequest;
import org.assessment.dto.response.AssignmentResponse;
import org.assessment.entity.Assignment;
import org.assessment.enums.AssignmentStatus;
import org.assessment.enums.AssignmentType;
import org.assessment.enums.DifficultyLevel;
import org.assessment.exception.ResourceNotFoundException;
import org.assessment.exception.ValidationException;
import org.assessment.mapper.AssignmentMapper;
import org.assessment.repository.AssignmentRepository;
import org.assessment.storage.S3Service;
import org.assessment.util.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentServiceImpl Tests")
class AssignmentServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentMapper assignmentMapper;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private Assignment sampleAssignment;
    private AssignmentResponse sampleResponse;
    private CreateAssignmentRequest createRequest;

    @BeforeEach
    void setUp() {
        sampleAssignment = Assignment.builder()
                .assignmentId("assign-001")
                .title("Java Basics")
                .description("Introduction to Java")
                .courseId("101")
                .createdBy("instructor-1")
                .assignmentType(AssignmentType.FILE_UPLOAD)
                .difficultyLevel(DifficultyLevel.BEGINNER)
                .status(AssignmentStatus.PUBLISHED)
                .totalMarks(100f)
                .passMarks(50f)
                .dueDate(LocalDate.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleResponse = AssignmentResponse.builder()
                .assignmentId("assign-001")
                .title("Java Basics")
                .courseId("101")
                .totalMarks(100f)
                .passMarks(50f)
                .status(AssignmentStatus.PUBLISHED)
                .build();

        createRequest = new CreateAssignmentRequest();
        createRequest.setTitle("Java Basics");
        createRequest.setCourseId("101");
        createRequest.setTotalMarks(100f);
        createRequest.setPassMarks(50f);
        createRequest.setAssignmentType(AssignmentType.FILE_UPLOAD);
        createRequest.setDifficultyLevel(DifficultyLevel.BEGINNER);
        createRequest.setDueDate(LocalDate.now().plusDays(7));
    }

    // -------------------------------------------------------------------------
    // createAssignment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("createAssignment")
    class CreateAssignment {

        @Test
        @DisplayName("should create assignment successfully without file")
        void createAssignment_success_noFile() {
            try (MockedStatic<CommonUtil> util = mockStatic(CommonUtil.class)) {
                util.when(CommonUtil::extractUserIdFromRequest).thenReturn("instructor-1");
                when(courseServiceClient.courseExists(101L)).thenReturn(true);
                when(assignmentMapper.toEntity(createRequest, "instructor-1")).thenReturn(sampleAssignment);
                when(assignmentRepository.save(sampleAssignment)).thenReturn(sampleAssignment);
                when(assignmentMapper.toResponse(sampleAssignment)).thenReturn(sampleResponse);

                AssignmentResponse result = assignmentService.createAssignment(createRequest, null);

                assertThat(result).isNotNull();
                assertThat(result.getAssignmentId()).isEqualTo("assign-001");
                verify(assignmentRepository).save(sampleAssignment);
                verify(s3Service, never()).uploadFile(any(), anyString());
            }
        }

        @Test
        @DisplayName("should create assignment and upload file when file is provided")
        void createAssignment_success_withFile() {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);

            try (MockedStatic<CommonUtil> util = mockStatic(CommonUtil.class)) {
                util.when(CommonUtil::extractUserIdFromRequest).thenReturn("instructor-1");
                when(courseServiceClient.courseExists(101L)).thenReturn(true);
                when(assignmentMapper.toEntity(createRequest, "instructor-1")).thenReturn(sampleAssignment);
                when(s3Service.uploadFile(mockFile, "assignments")).thenReturn("https://s3/assignments/file.pdf");
                when(assignmentRepository.save(sampleAssignment)).thenReturn(sampleAssignment);
                when(assignmentMapper.toResponse(sampleAssignment)).thenReturn(sampleResponse);

                AssignmentResponse result = assignmentService.createAssignment(createRequest, mockFile);

                assertThat(result).isNotNull();
                verify(s3Service).uploadFile(mockFile, "assignments");
                assertThat(sampleAssignment.getAssignmentFileUrl()).isEqualTo("https://s3/assignments/file.pdf");
            }
        }

        @Test
        @DisplayName("should throw ValidationException when courseId is null")
        void createAssignment_nullCourseId_throwsValidation() {
            createRequest.setCourseId(null);
            assertThatThrownBy(() -> assignmentService.createAssignment(createRequest, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Course ID must not be empty");
        }

        @Test
        @DisplayName("should throw ValidationException when courseId is blank")
        void createAssignment_blankCourseId_throwsValidation() {
            createRequest.setCourseId("   ");
            assertThatThrownBy(() -> assignmentService.createAssignment(createRequest, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Course ID must not be empty");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when course does not exist")
        void createAssignment_courseNotFound_throwsNotFound() {
            try (MockedStatic<CommonUtil> util = mockStatic(CommonUtil.class)) {
                util.when(CommonUtil::extractUserIdFromRequest).thenReturn("instructor-1");
                when(courseServiceClient.courseExists(101L)).thenReturn(false);

                assertThatThrownBy(() -> assignmentService.createAssignment(createRequest, null))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Course not found with id: 101");
            }
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when course returns null")
        void createAssignment_courseReturnsNull_throwsNotFound() {
            try (MockedStatic<CommonUtil> util = mockStatic(CommonUtil.class)) {
                util.when(CommonUtil::extractUserIdFromRequest).thenReturn("instructor-1");
                when(courseServiceClient.courseExists(101L)).thenReturn(null);

                assertThatThrownBy(() -> assignmentService.createAssignment(createRequest, null))
                        .isInstanceOf(ResourceNotFoundException.class);
            }
        }

        @Test
        @DisplayName("should throw ValidationException when courseId is not a valid number")
        void createAssignment_invalidCourseIdFormat_throwsValidation() {
            createRequest.setCourseId("not-a-number");
            assertThatThrownBy(() -> assignmentService.createAssignment(createRequest, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid course ID format");
        }

        @Test
        @DisplayName("should throw ValidationException when Feign call fails")
        void createAssignment_feignException_throwsValidation() {
            try (MockedStatic<CommonUtil> util = mockStatic(CommonUtil.class)) {
                util.when(CommonUtil::extractUserIdFromRequest).thenReturn("instructor-1");
                when(courseServiceClient.courseExists(101L))
                        .thenThrow(new RuntimeException("Service unavailable"));

                assertThatThrownBy(() -> assignmentService.createAssignment(createRequest, null))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("Failed to validate course existence");
            }
        }
    }

    // -------------------------------------------------------------------------
    // getAssignmentById
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getAssignmentById")
    class GetAssignmentById {

        @Test
        @DisplayName("should return assignment when found")
        void getAssignmentById_found() {
            when(assignmentRepository.findById("assign-001")).thenReturn(Optional.of(sampleAssignment));
            when(assignmentMapper.toResponse(sampleAssignment)).thenReturn(sampleResponse);

            AssignmentResponse result = assignmentService.getAssignmentById("assign-001");

            assertThat(result).isNotNull();
            assertThat(result.getAssignmentId()).isEqualTo("assign-001");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void getAssignmentById_notFound() {
            when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.getAssignmentById("missing"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Assignment not found with id: missing");
        }
    }

    // -------------------------------------------------------------------------
    // getAllAssignments
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getAllAssignments")
    class GetAllAssignments {

        @Test
        @DisplayName("should return all assignments")
        void getAllAssignments_returnsList() {
            when(assignmentRepository.findAll()).thenReturn(List.of(sampleAssignment));
            when(assignmentMapper.toResponse(sampleAssignment)).thenReturn(sampleResponse);

            List<AssignmentResponse> result = assignmentService.getAllAssignments();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssignmentId()).isEqualTo("assign-001");
        }

        @Test
        @DisplayName("should return empty list when no assignments exist")
        void getAllAssignments_emptyList() {
            when(assignmentRepository.findAll()).thenReturn(List.of());

            List<AssignmentResponse> result = assignmentService.getAllAssignments();

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getAssignmentsByCourse
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getAssignmentsByCourse")
    class GetAssignmentsByCourse {

        @Test
        @DisplayName("should return assignments for a given course")
        void getAssignmentsByCourse_returnsList() {
            when(assignmentRepository.findByCourseId("101")).thenReturn(List.of(sampleAssignment));
            when(assignmentMapper.toResponse(sampleAssignment)).thenReturn(sampleResponse);

            List<AssignmentResponse> result = assignmentService.getAssignmentsByCourse("101");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when course has no assignments")
        void getAssignmentsByCourse_empty() {
            when(assignmentRepository.findByCourseId("999")).thenReturn(List.of());

            List<AssignmentResponse> result = assignmentService.getAssignmentsByCourse("999");

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getAssignmentsByInstructor
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getAssignmentsByInstructor")
    class GetAssignmentsByInstructor {

        @Test
        @DisplayName("should return assignments created by instructor")
        void getAssignmentsByInstructor_returnsList() {
            when(assignmentRepository.findByCreatedBy("instructor-1")).thenReturn(List.of(sampleAssignment));
            when(assignmentMapper.toResponse(sampleAssignment)).thenReturn(sampleResponse);

            List<AssignmentResponse> result = assignmentService.getAssignmentsByInstructor("instructor-1");

            assertThat(result).hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // updateAssignment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("updateAssignment")
    class UpdateAssignment {

        @Test
        @DisplayName("should update assignment fields when all are provided")
        void updateAssignment_allFields() {
            UpdateAssignmentRequest req = new UpdateAssignmentRequest();
            req.setTitle("Advanced Java");
            req.setDescription("Deep dive");
            req.setAssignmentType(AssignmentType.PROJECT);
            req.setDifficultyLevel(DifficultyLevel.ADVANCED);
            req.setStatus(AssignmentStatus.CLOSED);
            req.setTotalMarks(200f);
            req.setPassMarks(100f);
            req.setDueDate(LocalDate.now().plusDays(14));

            when(assignmentRepository.findById("assign-001")).thenReturn(Optional.of(sampleAssignment));
            when(assignmentRepository.save(any(Assignment.class))).thenReturn(sampleAssignment);
            when(assignmentMapper.toResponse(any(Assignment.class))).thenReturn(sampleResponse);

            AssignmentResponse result = assignmentService.updateAssignment("assign-001", req, null);

            assertThat(result).isNotNull();
            verify(assignmentRepository).save(sampleAssignment);
            assertThat(sampleAssignment.getTitle()).isEqualTo("Advanced Java");
            assertThat(sampleAssignment.getTotalMarks()).isEqualTo(200f);
        }

        @Test
        @DisplayName("should upload new file and delete old file during update")
        void updateAssignment_withNewFile_deletesOldAndUploads() {
            sampleAssignment.setAssignmentFileUrl("https://s3/old-file.pdf");

            MultipartFile newFile = mock(MultipartFile.class);
            when(newFile.isEmpty()).thenReturn(false);

            UpdateAssignmentRequest req = new UpdateAssignmentRequest();

            when(assignmentRepository.findById("assign-001")).thenReturn(Optional.of(sampleAssignment));
            when(s3Service.uploadFile(newFile, "assignments")).thenReturn("https://s3/new-file.pdf");
            when(assignmentRepository.save(any())).thenReturn(sampleAssignment);
            when(assignmentMapper.toResponse(any())).thenReturn(sampleResponse);

            assignmentService.updateAssignment("assign-001", req, newFile);

            verify(s3Service).deleteFile("https://s3/old-file.pdf");
            verify(s3Service).uploadFile(newFile, "assignments");
            assertThat(sampleAssignment.getAssignmentFileUrl()).isEqualTo("https://s3/new-file.pdf");
        }

        @Test
        @DisplayName("should not delete old file when there was no previous file URL")
        void updateAssignment_noOldFile_doesNotDelete() {
            sampleAssignment.setAssignmentFileUrl(null);

            MultipartFile newFile = mock(MultipartFile.class);
            when(newFile.isEmpty()).thenReturn(false);

            UpdateAssignmentRequest req = new UpdateAssignmentRequest();

            when(assignmentRepository.findById("assign-001")).thenReturn(Optional.of(sampleAssignment));
            when(s3Service.uploadFile(newFile, "assignments")).thenReturn("https://s3/new-file.pdf");
            when(assignmentRepository.save(any())).thenReturn(sampleAssignment);
            when(assignmentMapper.toResponse(any())).thenReturn(sampleResponse);

            assignmentService.updateAssignment("assign-001", req, newFile);

            verify(s3Service, never()).deleteFile(anyString());
        }

        @Test
        @DisplayName("should skip file update when file is null")
        void updateAssignment_nullFile_skipsUpload() {
            UpdateAssignmentRequest req = new UpdateAssignmentRequest();
            req.setTitle("Updated Title");

            when(assignmentRepository.findById("assign-001")).thenReturn(Optional.of(sampleAssignment));
            when(assignmentRepository.save(any())).thenReturn(sampleAssignment);
            when(assignmentMapper.toResponse(any())).thenReturn(sampleResponse);

            assignmentService.updateAssignment("assign-001", req, null);

            verify(s3Service, never()).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when assignment not found")
        void updateAssignment_notFound() {
            when(assignmentRepository.findById("missing"))
                    .thenReturn(Optional.empty());

            UpdateAssignmentRequest request = new UpdateAssignmentRequest();

            assertThatThrownBy(() ->
                    assignmentService.updateAssignment("missing", request, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Assignment not found with id: missing");
        }

        // -------------------------------------------------------------------------
        // deleteAssignment
        // -------------------------------------------------------------------------
        @Nested
        @DisplayName("deleteAssignment")
        class DeleteAssignment {

            @Test
            @DisplayName("should delete assignment successfully")
            void deleteAssignment_success() {
                when(assignmentRepository.findById("assign-001")).thenReturn(Optional.of(sampleAssignment));

                assignmentService.deleteAssignment("assign-001");

                verify(assignmentRepository).deleteById("assign-001");
            }

            @Test
            @DisplayName("should throw ResourceNotFoundException when assignment not found")
            void deleteAssignment_notFound() {
                when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());

                // Extract the throwing call into a single-invocation lambda to satisfy Sonar JUnit rule
                Runnable deleteCall = () -> assignmentService.deleteAssignment("missing");

                assertThatThrownBy(deleteCall::run)
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Assignment not found with id: missing");
                verify(assignmentRepository, never()).deleteById(anyString());
            }
        }
    }
}
