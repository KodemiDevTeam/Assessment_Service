package org.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assessment.client.CourseServiceClient;
import org.assessment.dto.request.CreateAssignmentRequest;
import org.assessment.dto.request.UpdateAssignmentRequest;
import org.assessment.dto.response.AssignmentResponse;
import org.assessment.entity.Assignment;
import org.assessment.exception.ResourceNotFoundException;
import org.assessment.exception.ValidationException;
import org.assessment.mapper.AssignmentMapper;
import org.assessment.repository.AssignmentRepository;
import org.assessment.service.AssignmentService;
import org.assessment.storage.S3Service;
import org.assessment.util.CommonUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentServiceImpl implements AssignmentService {

    private static final String ASSIGNMENT_NOT_FOUND = "Assignment not found with id: ";

    private final AssignmentRepository assignmentRepository;
    private final AssignmentMapper assignmentMapper;
    private final CourseServiceClient courseServiceClient;
    private final S3Service s3Service;

    @Override
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, MultipartFile file) {
        if (request.getCourseId() == null || request.getCourseId().isBlank()) {
            throw new ValidationException("Course ID must not be empty");
        }

        try {
            Boolean exists = courseServiceClient.courseExists(Long.valueOf(request.getCourseId()));
            if (exists == null || !exists) {
                throw new ResourceNotFoundException("Course not found with id: " + request.getCourseId());
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid course ID format: " + request.getCourseId());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to check course existence via Feign", e);
            throw new ValidationException("Failed to validate course existence: " + e.getMessage());
        }

        String userId = CommonUtil.extractUserIdFromRequest();
        Assignment assignment = assignmentMapper.toEntity(request, userId);

        if (file != null && !file.isEmpty()) {
            String fileUrl = s3Service.uploadFile(file, "assignments");
            assignment.setAssignmentFileUrl(fileUrl);
        }

        Assignment saved = assignmentRepository.save(assignment);
        return assignmentMapper.toResponse(saved);
    }

    @Override
    public AssignmentResponse getAssignmentById(String assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_NOT_FOUND + assignmentId));
        return assignmentMapper.toResponse(assignment);
    }

    @Override
    public List<AssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll().stream()
                .map(assignmentMapper::toResponse)
                .toList();
    }

    @Override
    public List<AssignmentResponse> getAssignmentsByCourse(String courseId) {
        return assignmentRepository.findByCourseId(courseId).stream()
                .map(assignmentMapper::toResponse)
                .toList();
    }

    @Override
    public List<AssignmentResponse> getAssignmentsByInstructor(String instructorId) {
        return assignmentRepository.findByCreatedBy(instructorId).stream()
                .map(assignmentMapper::toResponse)
                .toList();
    }

    @Override
    public AssignmentResponse updateAssignment(String assignmentId, UpdateAssignmentRequest request, MultipartFile file) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_NOT_FOUND + assignmentId));

        if (request.getTitle() != null) {
            assignment.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            assignment.setDescription(request.getDescription());
        }
        if (request.getAssignmentType() != null) {
            assignment.setAssignmentType(request.getAssignmentType());
        }
        if (request.getDifficultyLevel() != null) {
            assignment.setDifficultyLevel(request.getDifficultyLevel());
        }
        if (request.getStatus() != null) {
            assignment.setStatus(request.getStatus());
        }
        if (request.getTotalMarks() != null) {
            assignment.setTotalMarks(request.getTotalMarks());
        }
        if (request.getPassMarks() != null) {
            assignment.setPassMarks(request.getPassMarks());
        }
        if (request.getDueDate() != null) {
            assignment.setDueDate(request.getDueDate());
        }

        if (file != null && !file.isEmpty()) {
            if (assignment.getAssignmentFileUrl() != null) {
                s3Service.deleteFile(assignment.getAssignmentFileUrl());
            }
            String fileUrl = s3Service.uploadFile(file, "assignments");
            assignment.setAssignmentFileUrl(fileUrl);
        }

        assignment.setUpdatedAt(LocalDateTime.now());
        Assignment saved = assignmentRepository.save(assignment);
        return assignmentMapper.toResponse(saved);
    }

    @Override
    public void deleteAssignment(String assignmentId) {
        // orElseThrow return value must be used — assign to variable to satisfy the check
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_NOT_FOUND + assignmentId));
        assignmentRepository.deleteById(assignment.getAssignmentId());
    }
}
