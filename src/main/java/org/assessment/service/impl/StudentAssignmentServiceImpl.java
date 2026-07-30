package org.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.assessment.dto.response.StudentAssignmentResponse;
import org.assessment.entity.Assignment;
import org.assessment.entity.Review;
import org.assessment.entity.Submission;
import org.assessment.enums.ResultStatus;
import org.assessment.enums.SubmissionStatus;
import org.assessment.exception.ResourceNotFoundException;
import org.assessment.repository.AssignmentRepository;
import org.assessment.repository.ReviewRepository;
import org.assessment.repository.SubmissionRepository;
import org.assessment.service.StudentAssignmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentAssignmentServiceImpl implements StudentAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public List<StudentAssignmentResponse> getMyAssignments(String studentId) {
        // Get all published assignments (student sees all published ones for their courses)
        return assignmentRepository.findAll().stream()
                .map(a -> buildStudentAssignmentResponse(a, studentId))
                .toList();
    }

    @Override
    public List<StudentAssignmentResponse> getMyAssignmentsByStatus(String studentId, SubmissionStatus status) {
        return getMyAssignments(studentId).stream()
                .filter(r -> status == null || status == r.getSubmissionStatus())
                .toList();
    }

    @Override
    public StudentAssignmentResponse getAssignmentDetail(String assignmentId, String studentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        return buildStudentAssignmentResponse(assignment, studentId);
    }

    // -------------------------------------------------------------------------
    // Core builder — merges assignment + submission + review into one response
    // -------------------------------------------------------------------------
    private StudentAssignmentResponse buildStudentAssignmentResponse(Assignment assignment, String studentId) {
        Optional<Submission> submissionOpt = submissionRepository
                .findByAssignmentIdAndLearnerId(assignment.getAssignmentId(), studentId);

        Optional<Review> reviewOpt = submissionOpt
                .flatMap(s -> reviewRepository.findBySubmissionId(s.getSubmissionId()));

        // Determine if overdue: past due date and not submitted
        boolean overdue = false;
        if (assignment.getDueDate() != null) {
            boolean pastDue = assignment.getDueDate().isBefore(LocalDate.now());
            boolean notSubmitted = submissionOpt.isEmpty() ||
                    submissionOpt.get().getStatus() == SubmissionStatus.NOT_SUBMITTED;
            overdue = pastDue && notSubmitted;
        }

        // Submission status — default NOT_SUBMITTED if no submission record
        SubmissionStatus submissionStatus = submissionOpt
                .map(Submission::getStatus)
                .orElse(SubmissionStatus.NOT_SUBMITTED);

        // Score percentage — e.g. 44/50 → 88%
        Float scorePercentage = null;
        Float marksAwarded = null;
        String feedback = null;
        ResultStatus resultStatus = ResultStatus.PENDING;
        String reviewedAt = null;

        if (reviewOpt.isPresent()) {
            Review review = reviewOpt.get();
            marksAwarded = review.getMarksAwarded();
            feedback = review.getFeedback();
            resultStatus = review.getResultStatus();
            reviewedAt = review.getReviewedAt() != null ? review.getReviewedAt().toString() : null;

            if (marksAwarded != null && assignment.getTotalMarks() != null && assignment.getTotalMarks() > 0) {
                scorePercentage = (marksAwarded / assignment.getTotalMarks()) * 100f;
                // Round to 1 decimal place
                scorePercentage = Math.round(scorePercentage * 10f) / 10f;
            }
        }

        // Extract just the filename from the URL for display
        String assignmentFileName = extractFileName(assignment.getAssignmentFileUrl());
        String submissionFileName = submissionOpt
                .map(s -> extractFileName(s.getSubmissionFileUrl()))
                .orElse(null);

        return StudentAssignmentResponse.builder()
                .assignmentId(assignment.getAssignmentId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .courseId(assignment.getCourseId())
                .courseName(assignment.getCourseName())
                .totalMarks(assignment.getTotalMarks())
                .passMarks(assignment.getPassMarks())
                .difficultyLevel(assignment.getDifficultyLevel())
                .assignmentStatus(assignment.getStatus())
                .dueDate(assignment.getDueDate() != null ? assignment.getDueDate().toString() : null)
                .createdAt(assignment.getCreatedAt() != null ? assignment.getCreatedAt().toString() : null)
                .overdue(overdue)
                .assignmentFileUrl(assignment.getAssignmentFileUrl())
                .assignmentFileName(assignmentFileName)
                .submissionId(submissionOpt.map(Submission::getSubmissionId).orElse(null))
                .submissionStatus(submissionStatus)
                .submittedAt(submissionOpt
                        .filter(s -> s.getSubmittedAt() != null)
                        .map(s -> s.getSubmittedAt().toString())
                        .orElse(null))
                .submissionFileUrl(submissionOpt.map(Submission::getSubmissionFileUrl).orElse(null))
                .submissionFileName(submissionFileName)
                .marksAwarded(marksAwarded)
                .scorePercentage(scorePercentage)
                .resultStatus(resultStatus)
                .feedback(feedback)
                .reviewedAt(reviewedAt)
                .build();
    }

    /**
     * Extracts the filename from a full S3 URL.
     * e.g. "https://bucket.s3.region.amazonaws.com/assignments/abc.pdf" → "abc.pdf"
     */
    private String extractFileName(String url) {
        if (url == null || url.isBlank()) return null;
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }
}
