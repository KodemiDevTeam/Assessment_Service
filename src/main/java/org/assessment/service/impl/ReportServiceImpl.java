package org.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.assessment.dto.response.ReportResponse;
import org.assessment.dto.response.SubmissionResponse;
import org.assessment.entity.Assignment;
import org.assessment.entity.Review;
import org.assessment.entity.Submission;
import org.assessment.enums.ResultStatus;
import org.assessment.enums.SubmissionStatus;
import org.assessment.exception.ResourceNotFoundException;
import org.assessment.mapper.SubmissionMapper;
import org.assessment.repository.AssignmentRepository;
import org.assessment.repository.ReviewRepository;
import org.assessment.repository.SubmissionRepository;
import org.assessment.service.ReportService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ReviewRepository reviewRepository;
    private final SubmissionMapper submissionMapper;

    @Override
    public ReportResponse getAssignmentReport(String assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        List<Submission> submissions = submissionRepository.findByAssignmentId(assignmentId);
        return buildReport(assignmentId, assignment, submissions);
    }

    @Override
    public List<ReportResponse> getCourseReport(String courseId) {
        return assignmentRepository.findByCourseId(courseId).stream()
                .map(a -> getAssignmentReport(a.getAssignmentId()))
                .toList();
    }

    @Override
    public byte[] exportReportAsCsv(String assignmentId) {
        ReportResponse report = getAssignmentReport(assignmentId);
        StringBuilder csv = new StringBuilder();
        csv.append("SubmissionId,StudentId,Status,ObtainedMarks,ResultStatus,SubmittedAt\n");
        report.getSubmissions().forEach(s ->
                csv.append(s.getId()).append(",")
                        .append(s.getStudentId()).append(",")
                        .append(s.getStatus() != null ? s.getStatus().name() : "").append(",")
                        .append(s.getObtainedMarks() != null ? s.getObtainedMarks() : "").append(",")
                        .append(s.getResultStatus() != null ? s.getResultStatus().name() : "").append(",")
                        .append(s.getSubmittedAt() != null ? s.getSubmittedAt() : "").append("\n")
        );
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Private helpers — extracted to reduce cognitive complexity of getAssignmentReport
    // -------------------------------------------------------------------------

    private ReportResponse buildReport(String assignmentId, Assignment assignment, List<Submission> submissions) {
        List<SubmissionResponse> submissionResponses = new ArrayList<>();
        ReportStats stats = new ReportStats();

        for (Submission submission : submissions) {
            Review review = reviewRepository.findBySubmissionId(submission.getSubmissionId()).orElse(null);
            submissionResponses.add(submissionMapper.toResponse(submission, review));
            updateCounts(stats, submission, review);
        }

        return toReportResponse(assignmentId, assignment, submissions.size(), stats, submissionResponses);
    }

    private void updateCounts(ReportStats stats, Submission submission, Review review) {
        if (submission.getStatus() != SubmissionStatus.NOT_SUBMITTED) {
            stats.submittedCount++;
        }

        if (submission.getStatus() == SubmissionStatus.REVIEWED) {
            stats.gradedCount++;
        } else {
            stats.pendingCount++;
        }

        updateScoreStats(stats, review);
    }

    private void updateScoreStats(ReportStats stats, Review review) {
        if (review == null) {
            return;
        }
        if (review.getResultStatus() == ResultStatus.PASS) {
            stats.passCount++;
        } else if (review.getResultStatus() == ResultStatus.FAIL) {
            stats.failCount++;
        }
        if (review.getMarksAwarded() != null) {
            stats.hasGrades = true;
            float marks = review.getMarksAwarded();
            stats.totalMarks += marks;
            if (marks > stats.highestScore) stats.highestScore = marks;
            if (marks < stats.lowestScore)  stats.lowestScore  = marks;
        }
    }

    private ReportResponse toReportResponse(String assignmentId, Assignment assignment,
                                             int totalStudents, ReportStats stats,
                                             List<SubmissionResponse> submissionResponses) {
        float averageScore   = (stats.hasGrades && stats.gradedCount > 0)
                ? stats.totalMarks / stats.gradedCount : 0.0f;
        float finalLowest    = stats.hasGrades ? stats.lowestScore : 0.0f;

        return ReportResponse.builder()
                .assignmentId(assignmentId)
                .assignmentTitle(assignment.getTitle())
                .dueDate(assignment.getDueDate() != null ? assignment.getDueDate().toString() : null)
                .status(assignment.getStatus())
                .totalStudents((long) totalStudents)
                .submittedCount(stats.submittedCount)
                .pendingCount(stats.pendingCount)
                .gradedCount(stats.gradedCount)
                .averageScore(averageScore)
                .highestScore(stats.highestScore)
                .lowestScore(finalLowest)
                .passCount(stats.passCount)
                .failCount(stats.failCount)
                .submissions(submissionResponses)
                .build();
    }

    /** Simple mutable accumulator — keeps the loop body flat. */
    private static class ReportStats {
        long  submittedCount = 0;
        long  gradedCount    = 0;
        long  pendingCount   = 0;
        long  passCount      = 0;
        long  failCount      = 0;
        float totalMarks     = 0.0f;
        float highestScore   = 0.0f;
        float lowestScore    = Float.MAX_VALUE;
        boolean hasGrades    = false;
    }
}
