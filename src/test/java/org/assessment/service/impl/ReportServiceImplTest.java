package org.assessment.service.impl;

import org.assessment.dto.response.ReportResponse;
import org.assessment.dto.response.SubmissionResponse;
import org.assessment.entity.Assignment;
import org.assessment.entity.Review;
import org.assessment.entity.Submission;
import org.assessment.enums.AssignmentStatus;
import org.assessment.enums.ResultStatus;
import org.assessment.enums.SubmissionStatus;
import org.assessment.exception.ResourceNotFoundException;
import org.assessment.mapper.SubmissionMapper;
import org.assessment.repository.AssignmentRepository;
import org.assessment.repository.ReviewRepository;
import org.assessment.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportServiceImpl Tests")
class ReportServiceImplTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private SubmissionMapper submissionMapper;

    @InjectMocks
    private ReportServiceImpl reportService;

    private static final String ASSIGNMENT_ID = "assign-001";
    private static final String COURSE_ID     = "course-101";

    private Assignment sampleAssignment;
    private Submission submittedSub;
    private Submission reviewedSub;
    private Review passReview;
    private Review failReview;

    @BeforeEach
    void setUp() {
        sampleAssignment = Assignment.builder()
                .assignmentId(ASSIGNMENT_ID)
                .title("Spring Boot Assignment")
                .courseId(COURSE_ID)
                .status(AssignmentStatus.PUBLISHED)
                .totalMarks(100f)
                .passMarks(50f)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        submittedSub = Submission.builder()
                .submissionId("sub-001").assignmentId(ASSIGNMENT_ID).learnerId("student-1")
                .status(SubmissionStatus.SUBMITTED).submittedAt(LocalDateTime.now()).build();

        reviewedSub = Submission.builder()
                .submissionId("sub-002").assignmentId(ASSIGNMENT_ID).learnerId("student-2")
                .status(SubmissionStatus.REVIEWED).submittedAt(LocalDateTime.now()).build();

        passReview = Review.builder()
                .reviewId("rev-001").submissionId("sub-002")
                .marksAwarded(80f).resultStatus(ResultStatus.PASS).build();

        failReview = Review.builder()
                .reviewId("rev-002").submissionId("sub-003")
                .marksAwarded(30f).resultStatus(ResultStatus.FAIL).build();
    }

    // -------------------------------------------------------------------------
    // getAssignmentReport
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getAssignmentReport")
    class GetAssignmentReport {

        @Test
        @DisplayName("should compute correct stats for mixed submissions with reviews")
        void getReport_mixedSubmissions() {
            SubmissionResponse sr1 = SubmissionResponse.builder()
                    .id("sub-001").status(SubmissionStatus.SUBMITTED).build();
            SubmissionResponse sr2 = SubmissionResponse.builder()
                    .id("sub-002").status(SubmissionStatus.REVIEWED).obtainedMarks(80f)
                    .resultStatus(ResultStatus.PASS).build();

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID))
                    .thenReturn(List.of(submittedSub, reviewedSub));
            when(reviewRepository.findBySubmissionId("sub-001")).thenReturn(Optional.empty());
            when(reviewRepository.findBySubmissionId("sub-002")).thenReturn(Optional.of(passReview));
            when(submissionMapper.toResponse(submittedSub, null)).thenReturn(sr1);
            when(submissionMapper.toResponse(reviewedSub, passReview)).thenReturn(sr2);

            ReportResponse report = reportService.getAssignmentReport(ASSIGNMENT_ID);

            assertThat(report.getAssignmentId()).isEqualTo(ASSIGNMENT_ID);
            assertThat(report.getAssignmentTitle()).isEqualTo("Spring Boot Assignment");
            assertThat(report.getTotalStudents()).isEqualTo(2L);
            assertThat(report.getSubmittedCount()).isEqualTo(2L);
            assertThat(report.getGradedCount()).isEqualTo(1L);
            assertThat(report.getPendingCount()).isEqualTo(1L);
            assertThat(report.getPassCount()).isEqualTo(1L);
            assertThat(report.getFailCount()).isZero();
            assertThat(report.getAverageScore()).isEqualTo(80f);
            assertThat(report.getHighestScore()).isEqualTo(80f);
            assertThat(report.getLowestScore()).isEqualTo(80f);
            assertThat(report.getSubmissions()).hasSize(2);
        }

        @Test
        @DisplayName("should compute averageScore as (sum of marks) / gradedCount")
        void getReport_averageScoreCalculation() {
            Submission sub3 = Submission.builder()
                    .submissionId("sub-003").assignmentId(ASSIGNMENT_ID).learnerId("student-3")
                    .status(SubmissionStatus.REVIEWED).build();
            Review review3 = Review.builder()
                    .reviewId("rev-003").submissionId("sub-003")
                    .marksAwarded(60f).resultStatus(ResultStatus.PASS).build();

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID))
                    .thenReturn(List.of(reviewedSub, sub3));
            when(reviewRepository.findBySubmissionId("sub-002")).thenReturn(Optional.of(passReview));
            when(reviewRepository.findBySubmissionId("sub-003")).thenReturn(Optional.of(review3));
            when(submissionMapper.toResponse(any(), any())).thenReturn(
                    SubmissionResponse.builder().id("x").build());

            ReportResponse report = reportService.getAssignmentReport(ASSIGNMENT_ID);

            assertThat(report.getAverageScore()).isEqualTo(70f);
            assertThat(report.getHighestScore()).isEqualTo(80f);
            assertThat(report.getLowestScore()).isEqualTo(60f);
            assertThat(report.getPassCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("should return zero scores when no submissions exist")
        void getReport_noSubmissions() {
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of());

            ReportResponse report = reportService.getAssignmentReport(ASSIGNMENT_ID);

            assertThat(report.getTotalStudents()).isZero();
            assertThat(report.getAverageScore()).isZero();
            assertThat(report.getHighestScore()).isZero();
            assertThat(report.getLowestScore()).isZero();
            assertThat(report.getPassCount()).isZero();
            assertThat(report.getFailCount()).isZero();
        }

        @Test
        @DisplayName("should count PASS and FAIL separately")
        void getReport_passFailCounts() {
            Submission failSub = Submission.builder()
                    .submissionId("sub-003").assignmentId(ASSIGNMENT_ID).learnerId("student-3")
                    .status(SubmissionStatus.REVIEWED).build();

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID))
                    .thenReturn(List.of(reviewedSub, failSub));
            when(reviewRepository.findBySubmissionId("sub-002")).thenReturn(Optional.of(passReview));
            when(reviewRepository.findBySubmissionId("sub-003")).thenReturn(Optional.of(failReview));
            when(submissionMapper.toResponse(any(), any())).thenReturn(
                    SubmissionResponse.builder().id("x").build());

            ReportResponse report = reportService.getAssignmentReport(ASSIGNMENT_ID);

            assertThat(report.getPassCount()).isEqualTo(1L);
            assertThat(report.getFailCount()).isEqualTo(1L);
            assertThat(report.getAverageScore()).isEqualTo(55f);
        }

        @Test
        @DisplayName("should include dueDate as string in report when set")
        void getReport_dueDateFormatted() {
            sampleAssignment.setDueDate(LocalDate.of(2025, 12, 31));

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of());

            ReportResponse report = reportService.getAssignmentReport(ASSIGNMENT_ID);

            assertThat(report.getDueDate()).isEqualTo("2025-12-31");
        }

        @Test
        @DisplayName("should have null dueDate in report when assignment has no dueDate")
        void getReport_nullDueDate() {
            sampleAssignment.setDueDate(null);

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of());

            ReportResponse report = reportService.getAssignmentReport(ASSIGNMENT_ID);

            assertThat(report.getDueDate()).isNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when assignment not found")
        void getReport_assignmentNotFound() {
            when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.getAssignmentReport("missing"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Assignment not found with id: missing");
        }

        @Test
        @DisplayName("should not increment submittedCount when submission is NOT_SUBMITTED")
        void getReport_submissionNotSubmittedStatus() {
            Submission notSubmitted = Submission.builder()
                    .submissionId("sub-003").assignmentId(ASSIGNMENT_ID).learnerId("student-3")
                    .status(SubmissionStatus.NOT_SUBMITTED).build();

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID))
                    .thenReturn(List.of(notSubmitted));
            when(reviewRepository.findBySubmissionId("sub-003")).thenReturn(Optional.empty());
            when(submissionMapper.toResponse(any(), any())).thenReturn(
                    SubmissionResponse.builder().id("sub-003").status(SubmissionStatus.NOT_SUBMITTED).build());

            ReportResponse report = reportService.getAssignmentReport(ASSIGNMENT_ID);

            assertThat(report.getTotalStudents()).isEqualTo(1L);
            assertThat(report.getSubmittedCount()).isZero();
            assertThat(report.getGradedCount()).isZero();
            assertThat(report.getPendingCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should handle review with null marksAwarded or other result status")
        void getReport_reviewMarksAwardedNull() {
            Submission sub = Submission.builder()
                    .submissionId("sub-004").assignmentId(ASSIGNMENT_ID).status(SubmissionStatus.REVIEWED).build();
            Review review = Review.builder()
                    .reviewId("rev-004").submissionId("sub-004").marksAwarded(null).resultStatus(null).build();

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of(sub));
            when(reviewRepository.findBySubmissionId("sub-004")).thenReturn(Optional.of(review));
            when(submissionMapper.toResponse(any(), any())).thenReturn(
                    SubmissionResponse.builder().id("sub-004").status(SubmissionStatus.REVIEWED).build());

            ReportResponse report = reportService.getAssignmentReport(ASSIGNMENT_ID);

            assertThat(report.getGradedCount()).isEqualTo(1L);
            assertThat(report.getAverageScore()).isZero();
            assertThat(report.getHighestScore()).isZero();
            assertThat(report.getLowestScore()).isZero();
        }
    }

    // -------------------------------------------------------------------------
    // getCourseReport
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getCourseReport")
    class GetCourseReport {

        @Test
        @DisplayName("should return a report for each assignment in the course")
        void getCourseReport_multipleAssignments() {
            Assignment assign2 = Assignment.builder()
                    .assignmentId("assign-002").courseId(COURSE_ID)
                    .title("Second Assignment").status(AssignmentStatus.PUBLISHED).build();

            when(assignmentRepository.findByCourseId(COURSE_ID))
                    .thenReturn(List.of(sampleAssignment, assign2));
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(assignmentRepository.findById("assign-002")).thenReturn(Optional.of(assign2));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of());
            when(submissionRepository.findByAssignmentId("assign-002")).thenReturn(List.of());

            List<ReportResponse> reports = reportService.getCourseReport(COURSE_ID);

            assertThat(reports)
                    .hasSize(2)
                    .extracting(ReportResponse::getAssignmentId)
                    .containsExactlyInAnyOrder(ASSIGNMENT_ID, "assign-002");
        }

        @Test
        @DisplayName("should return empty list when course has no assignments")
        void getCourseReport_noAssignments() {
            when(assignmentRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());

            List<ReportResponse> reports = reportService.getCourseReport(COURSE_ID);

            assertThat(reports).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // exportReportAsCsv
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("exportReportAsCsv")
    class ExportReportAsCsv {

        @Test
        @DisplayName("should return CSV bytes with header row")
        void exportCsv_hasHeaderRow() {
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of());

            byte[] csvBytes = reportService.exportReportAsCsv(ASSIGNMENT_ID);
            String csv = new String(csvBytes);

            assertThat(csv).startsWith("SubmissionId,StudentId,Status,ObtainedMarks,ResultStatus,SubmittedAt");
        }

        @Test
        @DisplayName("should include submission data in CSV rows")
        void exportCsv_hasSubmissionRows() {
            SubmissionResponse sr = SubmissionResponse.builder()
                    .id("sub-001").studentId("student-1")
                    .status(SubmissionStatus.REVIEWED).obtainedMarks(80f)
                    .resultStatus(ResultStatus.PASS).submittedAt("2025-01-15T10:00:00")
                    .build();

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of(reviewedSub));
            when(reviewRepository.findBySubmissionId("sub-002")).thenReturn(Optional.of(passReview));
            when(submissionMapper.toResponse(reviewedSub, passReview)).thenReturn(sr);

            byte[] csvBytes = reportService.exportReportAsCsv(ASSIGNMENT_ID);
            String csv = new String(csvBytes);

            assertThat(csv)
                    .contains("sub-001")
                    .contains("student-1")
                    .contains("REVIEWED")
                    .contains("80.0")
                    .contains("PASS");
        }

        @Test
        @DisplayName("should return valid CSV with only header when no submissions")
        void exportCsv_noSubmissions_onlyHeader() {
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of());

            byte[] csvBytes = reportService.exportReportAsCsv(ASSIGNMENT_ID);
            String csv = new String(csvBytes);

            assertThat(csv.lines().count()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should be UTF-8 encoded")
        void exportCsv_isUtf8() {
            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of());

            byte[] csvBytes = reportService.exportReportAsCsv(ASSIGNMENT_ID);

            assertThat(csvBytes).isNotEmpty();
            String decoded = new String(csvBytes, java.nio.charset.StandardCharsets.UTF_8);
            assertThat(decoded).isNotBlank();
        }

        @Test
        @DisplayName("should handle null fields in submission response when exporting CSV")
        void exportCsv_nullFields_emptyValuesInCsv() {
            SubmissionResponse srNulls = SubmissionResponse.builder()
                    .id("sub-null").studentId("student-null")
                    .status(null).obtainedMarks(null).resultStatus(null).submittedAt(null)
                    .build();

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(sampleAssignment));
            when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of(submittedSub));
            when(reviewRepository.findBySubmissionId("sub-001")).thenReturn(Optional.empty());
            when(submissionMapper.toResponse(submittedSub, null)).thenReturn(srNulls);

            byte[] csvBytes = reportService.exportReportAsCsv(ASSIGNMENT_ID);
            String csv = new String(csvBytes);

            assertThat(csv).contains("sub-null,student-null,,,,");
        }
    }
}
