package org.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assessment.client.EnrollmentServiceClient;
import org.assessment.dto.request.SubmitAssignmentRequest;
import org.assessment.dto.response.SubmissionResponse;
import org.assessment.entity.Assignment;
import org.assessment.entity.Review;
import org.assessment.entity.Submission;
import org.assessment.enums.AssignmentStatus;
import org.assessment.enums.AssignmentType;
import org.assessment.exception.ResourceNotFoundException;
import org.assessment.exception.ValidationException;
import org.assessment.mapper.SubmissionMapper;
import org.assessment.repository.AssignmentRepository;
import org.assessment.repository.ReviewRepository;
import org.assessment.repository.SubmissionRepository;
import org.assessment.service.SubmissionService;
import org.assessment.storage.S3Service;
import org.assessment.util.CommonUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final ReviewRepository reviewRepository;
    private final SubmissionMapper submissionMapper;
    private final S3Service s3Service;
    private final EnrollmentServiceClient enrollmentServiceClient;

    @Override
    public SubmissionResponse submitAssignment(SubmitAssignmentRequest request, MultipartFile file) {
        String studentId = CommonUtil.extractUserIdFromRequest();
        
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + request.getAssignmentId()));

        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new ValidationException("Assignment is not open for submissions");
        }

        // Validate student enrollment via Feign
        try {
            Boolean enrolled = enrollmentServiceClient.isEnrolled(Long.valueOf(studentId), Long.valueOf(assignment.getCourseId()));
            if (enrolled == null || !enrolled) {
                throw new ValidationException("Student is not enrolled in course: " + assignment.getCourseId());
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid student or course ID format");
        } catch (Exception e) {
            log.warn("Failed to check enrollment via Feign", e);
            throw new ValidationException("Failed to validate student enrollment: " + e.getMessage());
        }

        submissionRepository.findByAssignmentIdAndLearnerId(request.getAssignmentId(), studentId)
                .ifPresent(s -> {
                    throw new ValidationException("Already submitted for this assignment");
                });

        Submission submission = submissionMapper.toEntity(request, studentId);

        if (assignment.getAssignmentType() == AssignmentType.FILE_UPLOAD && file != null && !file.isEmpty()) {
            String fileUrl = s3Service.uploadFile(file, "submissions");
            submission.setSubmissionFileUrl(fileUrl);
        }

        Submission saved = submissionRepository.save(submission);
        return submissionMapper.toResponse(saved);
    }

    @Override
    public SubmissionResponse getSubmissionById(String id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + id));
        Review review = reviewRepository.findBySubmissionId(id).orElse(null);
        return submissionMapper.toResponse(submission, review);
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByAssignment(String assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId).stream()
                .map(s -> {
                    Review review = reviewRepository.findBySubmissionId(s.getSubmissionId()).orElse(null);
                    return submissionMapper.toResponse(s, review);
                })
                .toList();
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByStudent(String studentId) {
        return submissionRepository.findByLearnerId(studentId).stream()
                .map(s -> {
                    Review review = reviewRepository.findBySubmissionId(s.getSubmissionId()).orElse(null);
                    return submissionMapper.toResponse(s, review);
                })
                .toList();
    }

    @Override
    public SubmissionResponse getSubmissionByAssignmentAndStudent(String assignmentId, String studentId) {
        Submission submission = submissionRepository.findByAssignmentIdAndLearnerId(assignmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        Review review = reviewRepository.findBySubmissionId(submission.getSubmissionId()).orElse(null);
        return submissionMapper.toResponse(submission, review);
    }
}
