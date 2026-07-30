package org.assessment.service.impl;

import org.assessment.dto.response.DashboardResponse;
import org.assessment.entity.Assignment;
import org.assessment.entity.Review;
import org.assessment.entity.Submission;
import org.assessment.enums.AssignmentStatus;
import org.assessment.enums.ResultStatus;
import org.assessment.enums.SubmissionStatus;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl Tests")
class DashboardServiceImplTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private static final String INSTRUCTOR_ID = "instructor-1";
    private static final String STUDENT_ID    = "student-1";
    private static final String COURSE_ID     = "course-101";

    private Assignment assignment1;
    private Assignment assignment2;

    @BeforeEach
    void setUp() {
        assignment1 = Assignment.builder()
                .assignmentId("assign-001")
                .courseId(COURSE_ID)
                .createdBy(INSTRUCTOR_ID)
                .status(AssignmentStatus.PUBLISHED)
                .dueDate(LocalDate.now().plusDays(7))
                .totalMarks(100f)
                .passMarks(50f)
                .build();

        assignment2 = Assignment.builder()
                .assignmentId("assign-002")
                .courseId(COURSE_ID)
                .createdBy(INSTRUCTOR_ID)
                .status(AssignmentStatus.PUBLISHED)
                .dueDate(LocalDate.now().plusDays(14))
                .totalMarks(50f)
                .passMarks(25f)
                .build();
    }

    // -------------------------------------------------------------------------
    // getInstructorDashboard
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getInstructorDashboard")
    class GetInstructorDashboard {

        @Test
        @DisplayName("should aggregate counts for instructor with reviewed and pending submissions")
        void instructorDashboard_mixedSubmissions() {
            Submission reviewed = Submission.builder()
                    .submissionId("sub-001").assignmentId("assign-001").learnerId("student-1")
                    .status(SubmissionStatus.REVIEWED).build();
            Submission submitted = Submission.builder()
                    .submissionId("sub-002").assignmentId("assign-001").learnerId("student-2")
                    .status(SubmissionStatus.SUBMITTED).build();

            when(assignmentRepository.findByCreatedBy(INSTRUCTOR_ID))
                    .thenReturn(List.of(assignment1, assignment2));
            when(submissionRepository.findByAssignmentId("assign-001"))
                    .thenReturn(List.of(reviewed, submitted));
            when(submissionRepository.findByAssignmentId("assign-002"))
                    .thenReturn(List.of());

            DashboardResponse response = dashboardService.getInstructorDashboard(INSTRUCTOR_ID);

            assertThat(response.getTotalAssignments()).isEqualTo(2L);
            assertThat(response.getTotalSubmissions()).isEqualTo(2L);
            assertThat(response.getGradedSubmissions()).isEqualTo(1L);
            assertThat(response.getPendingReviews()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return all zeros when instructor has no assignments")
        void instructorDashboard_noAssignments() {
            when(assignmentRepository.findByCreatedBy(INSTRUCTOR_ID)).thenReturn(List.of());

            DashboardResponse response = dashboardService.getInstructorDashboard(INSTRUCTOR_ID);

            assertThat(response.getTotalAssignments()).isZero();
            assertThat(response.getTotalSubmissions()).isZero();
            assertThat(response.getPendingReviews()).isZero();
            assertThat(response.getGradedSubmissions()).isZero();
        }

        @Test
        @DisplayName("should count UNDER_REVIEW submission as pending (not graded)")
        void instructorDashboard_underReviewCountsAsPending() {
            Submission underReview = Submission.builder()
                    .submissionId("sub-001").assignmentId("assign-001")
                    .status(SubmissionStatus.UNDER_REVIEW).build();

            when(assignmentRepository.findByCreatedBy(INSTRUCTOR_ID)).thenReturn(List.of(assignment1));
            when(submissionRepository.findByAssignmentId("assign-001")).thenReturn(List.of(underReview));

            DashboardResponse response = dashboardService.getInstructorDashboard(INSTRUCTOR_ID);

            assertThat(response.getPendingReviews()).isEqualTo(1L);
            assertThat(response.getGradedSubmissions()).isZero();
        }

        @Test
        @DisplayName("should aggregate submissions across multiple assignments")
        void instructorDashboard_multipleAssignmentsWithSubmissions() {
            Submission s1 = Submission.builder().submissionId("s1").assignmentId("assign-001")
                    .status(SubmissionStatus.REVIEWED).build();
            Submission s2 = Submission.builder().submissionId("s2").assignmentId("assign-002")
                    .status(SubmissionStatus.REVIEWED).build();

            when(assignmentRepository.findByCreatedBy(INSTRUCTOR_ID))
                    .thenReturn(List.of(assignment1, assignment2));
            when(submissionRepository.findByAssignmentId("assign-001")).thenReturn(List.of(s1));
            when(submissionRepository.findByAssignmentId("assign-002")).thenReturn(List.of(s2));

            DashboardResponse response = dashboardService.getInstructorDashboard(INSTRUCTOR_ID);

            assertThat(response.getTotalSubmissions()).isEqualTo(2L);
            assertThat(response.getGradedSubmissions()).isEqualTo(2L);
            assertThat(response.getPendingReviews()).isZero();
        }
    }

    // -------------------------------------------------------------------------
    // getStudentDashboard
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getStudentDashboard")
    class GetStudentDashboard {

        @Test
        @DisplayName("should compute averageScore, passCount, failCount correctly")
        void studentDashboard_withReviews() {
            Submission sub1 = Submission.builder()
                    .submissionId("sub-001").assignmentId("assign-001").learnerId(STUDENT_ID)
                    .status(SubmissionStatus.REVIEWED).submittedAt(LocalDateTime.now()).build();
            Submission sub2 = Submission.builder()
                    .submissionId("sub-002").assignmentId("assign-002").learnerId(STUDENT_ID)
                    .status(SubmissionStatus.REVIEWED).submittedAt(LocalDateTime.now()).build();

            Review reviewPass = Review.builder()
                    .reviewId("rev-001").submissionId("sub-001")
                    .marksAwarded(80f).resultStatus(ResultStatus.PASS).build();
            Review reviewFail = Review.builder()
                    .reviewId("rev-002").submissionId("sub-002")
                    .marksAwarded(40f).resultStatus(ResultStatus.FAIL).build();

            when(assignmentRepository.findAll()).thenReturn(List.of(assignment1, assignment2));
            when(submissionRepository.findByLearnerId(STUDENT_ID)).thenReturn(List.of(sub1, sub2));
            when(reviewRepository.findBySubmissionId("sub-001")).thenReturn(Optional.of(reviewPass));
            when(reviewRepository.findBySubmissionId("sub-002")).thenReturn(Optional.of(reviewFail));

            DashboardResponse response = dashboardService.getStudentDashboard(STUDENT_ID);

            assertThat(response.getTotalSubmissions()).isEqualTo(2L);
            assertThat(response.getGradedSubmissions()).isEqualTo(2L);
            assertThat(response.getPassCount()).isEqualTo(1L);
            assertThat(response.getFailCount()).isEqualTo(1L);
            assertThat(response.getAverageScore()).isEqualTo(60f);
            assertThat(response.getPendingOverdue()).isZero();
        }

        @Test
        @DisplayName("should count overdue when assignment is past due and student has not submitted")
        void studentDashboard_overdueCount() {
            Assignment overdueAssignment = Assignment.builder()
                    .assignmentId("assign-overdue")
                    .dueDate(LocalDate.now().minusDays(1))
                    .build();

            when(assignmentRepository.findAll()).thenReturn(List.of(overdueAssignment));
            when(submissionRepository.findByLearnerId(STUDENT_ID)).thenReturn(List.of());
            when(submissionRepository.findByAssignmentIdAndLearnerId("assign-overdue", STUDENT_ID))
                    .thenReturn(Optional.empty());

            DashboardResponse response = dashboardService.getStudentDashboard(STUDENT_ID);

            assertThat(response.getPendingOverdue()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should not count overdue when assignment is still in the future")
        void studentDashboard_futureDueDate_notOverdue() {
            Assignment futureAssignment = Assignment.builder()
                    .assignmentId("assign-future")
                    .dueDate(LocalDate.now().plusDays(7))
                    .build();

            when(assignmentRepository.findAll()).thenReturn(List.of(futureAssignment));
            when(submissionRepository.findByLearnerId(STUDENT_ID)).thenReturn(List.of());

            DashboardResponse response = dashboardService.getStudentDashboard(STUDENT_ID);

            assertThat(response.getPendingOverdue()).isZero();
        }

        @Test
        @DisplayName("should not count overdue when student already submitted a past-due assignment")
        void studentDashboard_submittedPastDue_notOverdue() {
            Assignment pastDue = Assignment.builder()
                    .assignmentId("assign-past")
                    .dueDate(LocalDate.now().minusDays(1))
                    .build();
            Submission alreadySubmitted = Submission.builder()
                    .submissionId("sub-001").assignmentId("assign-past").learnerId(STUDENT_ID)
                    .status(SubmissionStatus.SUBMITTED).build();

            when(assignmentRepository.findAll()).thenReturn(List.of(pastDue));
            when(submissionRepository.findByLearnerId(STUDENT_ID)).thenReturn(List.of(alreadySubmitted));
            when(submissionRepository.findByAssignmentIdAndLearnerId("assign-past", STUDENT_ID))
                    .thenReturn(Optional.of(alreadySubmitted));
            when(reviewRepository.findBySubmissionId("sub-001")).thenReturn(Optional.empty());

            DashboardResponse response = dashboardService.getStudentDashboard(STUDENT_ID);

            assertThat(response.getPendingOverdue()).isZero();
        }

        @Test
        @DisplayName("should return averageScore 0 when no submissions are graded")
        void studentDashboard_noGradedSubmissions_averageZero() {
            when(assignmentRepository.findAll()).thenReturn(List.of());
            when(submissionRepository.findByLearnerId(STUDENT_ID)).thenReturn(List.of());

            DashboardResponse response = dashboardService.getStudentDashboard(STUDENT_ID);

            assertThat(response.getAverageScore()).isZero();
            assertThat(response.getTotalSubmissions()).isZero();
        }

        @Test
        @DisplayName("should ignore overdue check when assignment has no due date")
        void studentDashboard_assignmentNoDueDate_notOverdue() {
            Assignment noDueDate = Assignment.builder()
                    .assignmentId("assign-nodue")
                    .dueDate(null)
                    .build();

            when(assignmentRepository.findAll()).thenReturn(List.of(noDueDate));
            when(submissionRepository.findByLearnerId(STUDENT_ID)).thenReturn(List.of());

            DashboardResponse response = dashboardService.getStudentDashboard(STUDENT_ID);

            assertThat(response.getPendingOverdue()).isZero();
        }

        @Test
        @DisplayName("should handle review with null marks or PENDING status correctly")
        void studentDashboard_reviewPendingOrNullMarks() {
            Submission sub = Submission.builder()
                    .submissionId("sub-001").assignmentId("assign-001").learnerId(STUDENT_ID)
                    .status(SubmissionStatus.REVIEWED).build();

            Review review = Review.builder()
                    .reviewId("rev-001").submissionId("sub-001")
                    .marksAwarded(null).resultStatus(null).build();

            when(assignmentRepository.findAll()).thenReturn(List.of(assignment1));
            when(submissionRepository.findByLearnerId(STUDENT_ID)).thenReturn(List.of(sub));
            when(reviewRepository.findBySubmissionId("sub-001")).thenReturn(Optional.of(review));

            DashboardResponse response = dashboardService.getStudentDashboard(STUDENT_ID);

            assertThat(response.getGradedSubmissions()).isEqualTo(1L);
            assertThat(response.getPassCount()).isZero();
            assertThat(response.getFailCount()).isZero();
            assertThat(response.getAverageScore()).isZero();
        }
    }

    // -------------------------------------------------------------------------
    // getCourseAssignmentStats
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getCourseAssignmentStats")
    class GetCourseAssignmentStats {

        @Test
        @DisplayName("should return total assignments and total submissions for a course")
        void courseStats_withData() {
            when(assignmentRepository.findByCourseId(COURSE_ID))
                    .thenReturn(List.of(assignment1, assignment2));
            when(submissionRepository.countByAssignmentId("assign-001")).thenReturn(3L);
            when(submissionRepository.countByAssignmentId("assign-002")).thenReturn(2L);

            DashboardResponse response = dashboardService.getCourseAssignmentStats(COURSE_ID);

            assertThat(response.getTotalAssignments()).isEqualTo(2L);
            assertThat(response.getTotalSubmissions()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should return zeros when course has no assignments")
        void courseStats_noAssignments() {
            when(assignmentRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());

            DashboardResponse response = dashboardService.getCourseAssignmentStats(COURSE_ID);

            assertThat(response.getTotalAssignments()).isZero();
            assertThat(response.getTotalSubmissions()).isZero();
        }

        @Test
        @DisplayName("should return correct count when assignments have no submissions")
        void courseStats_assignmentsWithNoSubmissions() {
            when(assignmentRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(assignment1));
            when(submissionRepository.countByAssignmentId("assign-001")).thenReturn(0L);

            DashboardResponse response = dashboardService.getCourseAssignmentStats(COURSE_ID);

            assertThat(response.getTotalAssignments()).isEqualTo(1L);
            assertThat(response.getTotalSubmissions()).isZero();
        }
    }
}
