package org.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.assessment.dto.request.ReviewAssignmentRequest;
import org.assessment.dto.response.ReviewResponse;
import org.assessment.entity.Assignment;
import org.assessment.entity.Review;
import org.assessment.entity.Submission;
import org.assessment.enums.ResultStatus;
import org.assessment.enums.SubmissionStatus;
import org.assessment.exception.ResourceNotFoundException;
import org.assessment.exception.ValidationException;
import org.assessment.mapper.AssignmentReviewMapper;
import org.assessment.repository.AssignmentRepository;
import org.assessment.repository.ReviewRepository;
import org.assessment.repository.SubmissionRepository;
import org.assessment.service.ReviewService;
import org.assessment.util.CommonUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentReviewMapper reviewMapper;

    @Override
    public ReviewResponse reviewSubmission(ReviewAssignmentRequest request) {
        String reviewerId = CommonUtil.extractUserIdFromRequest();

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + request.getSubmissionId()));

        reviewRepository.findBySubmissionId(request.getSubmissionId())
                .ifPresent(r -> {
                    throw new ValidationException("Submission already reviewed");
                });

        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + submission.getAssignmentId()));

        Review review = reviewMapper.toEntity(request, reviewerId);
        
        // Calculate result status based on pass marks
        float passMarks = assignment.getPassMarks() != null ? assignment.getPassMarks() : 0f;
        float marksAwarded = review.getMarksAwarded() != null ? review.getMarksAwarded() : 0f;
        review.setResultStatus(marksAwarded >= passMarks ? ResultStatus.PASS : ResultStatus.FAIL);
        
        Review savedReview = reviewRepository.save(review);

        submission.setStatus(SubmissionStatus.REVIEWED);
        submissionRepository.save(submission);

        return reviewMapper.toResponse(savedReview);
    }

    @Override
    public ReviewResponse getReviewBySubmission(String submissionId) {
        Review review = reviewRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for submission: " + submissionId));
        return reviewMapper.toResponse(review);
    }

    @Override
    public List<ReviewResponse> getReviewsByReviewer(String reviewerId) {
        return reviewRepository.findByReviewerId(reviewerId).stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Override
    public ReviewResponse updateReview(String reviewId, ReviewAssignmentRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        
        Submission submission = submissionRepository.findById(review.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found for this review"));
        
        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found for this review"));

        if (request.getFeedback() != null) {
            review.setFeedback(request.getFeedback());
        }
        if (request.getMarksAwarded() != null) {
            review.setMarksAwarded(request.getMarksAwarded());
            float passMarks = assignment.getPassMarks() != null ? assignment.getPassMarks() : 0f;
            review.setResultStatus(review.getMarksAwarded() >= passMarks ? ResultStatus.PASS : ResultStatus.FAIL);
        }
        review.setReviewedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);
        return reviewMapper.toResponse(savedReview);
    }
}
