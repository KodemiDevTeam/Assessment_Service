package org.assessment.dto;

import org.assessment.dto.request.*;
import org.assessment.dto.response.*;
import org.assessment.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTO Classes Tests")
class DtoTest {

    // =========================================================================
    // REQUEST DTOs
    // =========================================================================

    @Nested
    @DisplayName("CreateAssignmentRequest")
    class CreateAssignmentRequestTest {

        @Test
        @DisplayName("should set and get all fields via Lombok @Data")
        void setAndGetAllFields() {
            CreateAssignmentRequest r = new CreateAssignmentRequest();
            r.setTitle("Java Basics");
            r.setDescription("Learn Java");
            r.setCourseId("101");
            r.setTotalMarks(100f);
            r.setPassMarks(50f);
            r.setAssignmentType(AssignmentType.FILE_UPLOAD);
            r.setDifficultyLevel(DifficultyLevel.BEGINNER);
            r.setDueDate(LocalDate.of(2025, 12, 31));

            assertThat(r.getTitle()).isEqualTo("Java Basics");
            assertThat(r.getDescription()).isEqualTo("Learn Java");
            assertThat(r.getCourseId()).isEqualTo("101");
            assertThat(r.getTotalMarks()).isEqualTo(100f);
            assertThat(r.getPassMarks()).isEqualTo(50f);
            assertThat(r.getAssignmentType()).isEqualTo(AssignmentType.FILE_UPLOAD);
            assertThat(r.getDifficultyLevel()).isEqualTo(DifficultyLevel.BEGINNER);
            assertThat(r.getDueDate()).isEqualTo(LocalDate.of(2025, 12, 31));
        }

        @Test
        @DisplayName("default constructor should produce null fields")
        void defaultConstructor_nullFields() {
            CreateAssignmentRequest r = new CreateAssignmentRequest();
            assertThat(r.getTitle()).isNull();
            assertThat(r.getCourseId()).isNull();
            assertThat(r.getTotalMarks()).isNull();
        }

        @Test
        @DisplayName("equals and hashCode should work for identical objects")
        void equalsAndHashCode() {
            CreateAssignmentRequest r1 = new CreateAssignmentRequest();
            r1.setTitle("Test");
            r1.setCourseId("1");
            r1.setTotalMarks(50f);
            r1.setPassMarks(25f);

            CreateAssignmentRequest r2 = new CreateAssignmentRequest();
            r2.setTitle("Test");
            r2.setCourseId("1");
            r2.setTotalMarks(50f);
            r2.setPassMarks(25f);

            assertThat(r1)
                    .isEqualTo(r2)
                    .hasSameHashCodeAs(r2);
        }
    }

    @Nested
    @DisplayName("UpdateAssignmentRequest")
    class UpdateAssignmentRequestTest {

        @Test
        @DisplayName("should set and get all fields")
        void setAndGetAllFields() {
            UpdateAssignmentRequest r = new UpdateAssignmentRequest();
            r.setTitle("Advanced Java");
            r.setDescription("Deep dive");
            r.setAssignmentType(AssignmentType.PROJECT);
            r.setDifficultyLevel(DifficultyLevel.ADVANCED);
            r.setStatus(AssignmentStatus.CLOSED);
            r.setTotalMarks(200f);
            r.setPassMarks(100f);
            r.setDueDate(LocalDate.of(2026, 6, 1));
            r.setAllowLateSubmission(true);

            assertThat(r.getTitle()).isEqualTo("Advanced Java");
            assertThat(r.getStatus()).isEqualTo(AssignmentStatus.CLOSED);
            assertThat(r.getAllowLateSubmission()).isTrue();
            assertThat(r.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        }

        @Test
        @DisplayName("all fields should default to null")
        void defaultsToNull() {
            UpdateAssignmentRequest r = new UpdateAssignmentRequest();
            assertThat(r.getTitle()).isNull();
            assertThat(r.getStatus()).isNull();
            assertThat(r.getAllowLateSubmission()).isNull();
        }
    }

    @Nested
    @DisplayName("SubmitAssignmentRequest")
    class SubmitAssignmentRequestTest {

        @Test
        @DisplayName("should set and get all fields")
        void setAndGetAllFields() {
            SubmitAssignmentRequest r = new SubmitAssignmentRequest();
            r.setAssignmentId("assign-001");
            r.setContent("My answer here");
            r.setFileUrl("https://s3/solution.pdf");

            assertThat(r.getAssignmentId()).isEqualTo("assign-001");
            assertThat(r.getContent()).isEqualTo("My answer here");
            assertThat(r.getFileUrl()).isEqualTo("https://s3/solution.pdf");
        }

        @Test
        @DisplayName("optional fields default to null")
        void optionalFieldsNull() {
            SubmitAssignmentRequest r = new SubmitAssignmentRequest();
            r.setAssignmentId("assign-001");

            assertThat(r.getContent()).isNull();
            assertThat(r.getFileUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("ReviewAssignmentRequest")
    class ReviewAssignmentRequestTest {

        @Test
        @DisplayName("should set and get all fields")
        void setAndGetAllFields() {
            ReviewAssignmentRequest r = new ReviewAssignmentRequest();
            r.setSubmissionId("sub-001");
            r.setFeedback("Good work");
            r.setMarksAwarded(85f);

            assertThat(r.getSubmissionId()).isEqualTo("sub-001");
            assertThat(r.getFeedback()).isEqualTo("Good work");
            assertThat(r.getMarksAwarded()).isEqualTo(85f);
        }

        @Test
        @DisplayName("feedback can be null (optional)")
        void feedbackIsOptional() {
            ReviewAssignmentRequest r = new ReviewAssignmentRequest();
            r.setSubmissionId("sub-001");
            r.setMarksAwarded(70f);

            assertThat(r.getFeedback()).isNull();
        }
    }

    // =========================================================================
    // RESPONSE DTOs
    // =========================================================================

    @Nested
    @DisplayName("AssignmentResponse")
    class AssignmentResponseTest {

        @Test
        @DisplayName("should build with all fields via Lombok @Builder")
        void builder_setsAllFields() {
            AssignmentResponse r = AssignmentResponse.builder()
                    .assignmentId("a-001")
                    .title("Java Basics")
                    .description("Intro")
                    .courseId("101")
                    .courseName("Java Course")
                    .totalMarks(100f)
                    .passMarks(50f)
                    .assignmentType(AssignmentType.FILE_UPLOAD)
                    .difficultyLevel(DifficultyLevel.BEGINNER)
                    .status(AssignmentStatus.PUBLISHED)
                    .dueDate("2025-12-31")
                    .assignmentFileUrl("https://s3/f.pdf")
                    .createdBy("inst-1")
                    .createdAt("2025-01-01T10:00")
                    .updatedAt("2025-01-02T10:00")
                    .build();

            assertThat(r.getAssignmentId()).isEqualTo("a-001");
            assertThat(r.getTitle()).isEqualTo("Java Basics");
            assertThat(r.getStatus()).isEqualTo(AssignmentStatus.PUBLISHED);
            assertThat(r.getTotalMarks()).isEqualTo(100f);
            assertThat(r.getDueDate()).isEqualTo("2025-12-31");
        }

        @Test
        @DisplayName("equals and hashCode via @Data")
        void equalsAndHashCode() {
            AssignmentResponse r1 = AssignmentResponse.builder().assignmentId("a1").title("T").build();
            AssignmentResponse r2 = AssignmentResponse.builder().assignmentId("a1").title("T").build();
            assertThat(r1)
                    .isEqualTo(r2)
                    .hasSameHashCodeAs(r2);
        }
    }

    @Nested
    @DisplayName("SubmissionResponse")
    class SubmissionResponseTest {

        @Test
        @DisplayName("should build with all fields")
        void builder_setsAllFields() {
            SubmissionResponse r = SubmissionResponse.builder()
                    .id("sub-001")
                    .assignmentId("a-001")
                    .studentId("s-1")
                    .studentName("Alice")
                    .content("answer")
                    .fileUrl("https://s3/sub.pdf")
                    .status(SubmissionStatus.REVIEWED)
                    .resultStatus(ResultStatus.PASS)
                    .obtainedMarks(88f)
                    .feedback("Well done")
                    .submittedAt("2025-06-01T09:00")
                    .createdAt("2025-06-01T09:00")
                    .updatedAt("2025-06-01T09:00")
                    .build();

            assertThat(r.getId()).isEqualTo("sub-001");
            assertThat(r.getStudentName()).isEqualTo("Alice");
            assertThat(r.getObtainedMarks()).isEqualTo(88f);
            assertThat(r.getResultStatus()).isEqualTo(ResultStatus.PASS);
        }
    }

    @Nested
    @DisplayName("ReviewResponse")
    class ReviewResponseTest {

        @Test
        @DisplayName("should build with all fields")
        void builder_setsAllFields() {
            ReviewResponse r = ReviewResponse.builder()
                    .id("rev-001")
                    .submissionId("sub-001")
                    .reviewerId("inst-1")
                    .feedback("Great")
                    .marksAwarded(92f)
                    .resultStatus(ResultStatus.PASS)
                    .reviewedAt("2025-07-01T14:00")
                    .createdAt("2025-07-01T14:00")
                    .updatedAt("2025-07-01T14:00")
                    .build();

            assertThat(r.getId()).isEqualTo("rev-001");
            assertThat(r.getMarksAwarded()).isEqualTo(92f);
            assertThat(r.getResultStatus()).isEqualTo(ResultStatus.PASS);
        }
    }

    @Nested
    @DisplayName("DashboardResponse")
    class DashboardResponseTest {

        @Test
        @DisplayName("should build instructor dashboard fields")
        void builder_instructorFields() {
            DashboardResponse r = DashboardResponse.builder()
                    .totalAssignments(5L)
                    .totalSubmissions(20L)
                    .pendingReviews(8L)
                    .gradedSubmissions(12L)
                    .build();

            assertThat(r.getTotalAssignments()).isEqualTo(5L);
            assertThat(r.getPendingReviews()).isEqualTo(8L);
            assertThat(r.getGradedSubmissions()).isEqualTo(12L);
        }

        @Test
        @DisplayName("should build student dashboard fields")
        void builder_studentFields() {
            DashboardResponse r = DashboardResponse.builder()
                    .totalAssignments(10L)
                    .submittedCount(6L)
                    .reviewedCount(4L)
                    .pendingOverdue(2L)
                    .passCount(3L)
                    .failCount(1L)
                    .averageScore(75.5f)
                    .build();

            assertThat(r.getSubmittedCount()).isEqualTo(6L);
            assertThat(r.getPendingOverdue()).isEqualTo(2L);
            assertThat(r.getAverageScore()).isEqualTo(75.5f);
        }
    }

    @Nested
    @DisplayName("ReportResponse")
    class ReportResponseTest {

        @Test
        @DisplayName("should build with all fields including submissions list")
        void builder_setsAllFields() {
            SubmissionResponse sub = SubmissionResponse.builder().id("s1").build();
            ReportResponse r = ReportResponse.builder()
                    .assignmentId("a-001")
                    .assignmentTitle("Java Basics")
                    .dueDate("2025-12-31")
                    .status(AssignmentStatus.PUBLISHED)
                    .totalStudents(10L)
                    .submittedCount(8L)
                    .pendingCount(2L)
                    .gradedCount(6L)
                    .averageScore(74f)
                    .highestScore(95f)
                    .lowestScore(45f)
                    .passCount(5L)
                    .failCount(1L)
                    .submissions(List.of(sub))
                    .build();

            assertThat(r.getAssignmentId()).isEqualTo("a-001");
            assertThat(r.getTotalStudents()).isEqualTo(10L);
            assertThat(r.getAverageScore()).isEqualTo(74f);
            assertThat(r.getSubmissions()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("StudentAssignmentResponse")
    class StudentAssignmentResponseTest {

        @Test
        @DisplayName("should build with all assignment and submission fields")
        void builder_setsAllFields() {
            StudentAssignmentResponse r = StudentAssignmentResponse.builder()
                    .assignmentId("a-001")
                    .title("Java REST API")
                    .description("Build REST API")
                    .courseId("c-101")
                    .courseName("Java Course")
                    .totalMarks(50f)
                    .passMarks(25f)
                    .difficultyLevel(DifficultyLevel.INTERMEDIATE)
                    .assignmentStatus(AssignmentStatus.PUBLISHED)
                    .dueDate("2025-12-31")
                    .createdAt("2025-01-01")
                    .overdue(false)
                    .assignmentFileUrl("https://s3/a.pdf")
                    .assignmentFileName("a.pdf")
                    .submissionId("sub-001")
                    .submissionStatus(SubmissionStatus.REVIEWED)
                    .submittedAt("2025-06-01")
                    .submissionFileUrl("https://s3/s.pdf")
                    .submissionFileName("s.pdf")
                    .marksAwarded(44f)
                    .scorePercentage(88f)
                    .resultStatus(ResultStatus.PASS)
                    .feedback("Well done")
                    .reviewedAt("2025-07-01")
                    .build();

            assertThat(r.getAssignmentId()).isEqualTo("a-001");
            assertThat(r.getScorePercentage()).isEqualTo(88f);
            assertThat(r.getResultStatus()).isEqualTo(ResultStatus.PASS);
            assertThat(r.isOverdue()).isFalse();
            assertThat(r.getSubmissionStatus()).isEqualTo(SubmissionStatus.REVIEWED);
        }

        @Test
        @DisplayName("overdue boolean should be true when explicitly set")
        void overdueFlag_true() {
            StudentAssignmentResponse r = StudentAssignmentResponse.builder()
                    .assignmentId("a-001").title("T")
                    .overdue(true)
                    .resultStatus(ResultStatus.PENDING)
                    .build();

            assertThat(r.isOverdue()).isTrue();
        }
    }
}
