package org.assessment.repository;

import org.assessment.entity.Submission;
import org.assessment.enums.SubmissionStatus;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Optional;

@Repository
public class SubmissionRepository {

    private final DynamoDbTable<Submission> table;

    public SubmissionRepository(DynamoDbEnhancedClient enhancedClient) {
        // Table name "submissions" and partition key "submissionId"
        // are defined directly in Submission entity via @DynamoDbTableName and @DynamoDbPartitionKey
        this.table = enhancedClient.table("submissions", TableSchema.fromBean(Submission.class));
    }

    public Submission save(Submission submission) {
        table.putItem(submission);
        return submission;
    }

    public Optional<Submission> findById(String submissionId) {
        return Optional.ofNullable(table.getItem(Key.builder().partitionValue(submissionId).build()));
    }

    public List<Submission> findByAssignmentId(String assignmentId) {
        return table.scan().items().stream()
                .filter(s -> assignmentId.equals(s.getAssignmentId()))
                .toList();
    }

    public List<Submission> findByLearnerId(String learnerId) {
        return table.scan().items().stream()
                .filter(s -> learnerId.equals(s.getLearnerId()))
                .toList();
    }

    public Optional<Submission> findByAssignmentIdAndLearnerId(String assignmentId, String learnerId) {
        return table.scan().items().stream()
                .filter(s -> assignmentId.equals(s.getAssignmentId()) && learnerId.equals(s.getLearnerId()))
                .findFirst();
    }

    public List<Submission> findByAssignmentIdAndStatus(String assignmentId, SubmissionStatus status) {
        return table.scan().items().stream()
                .filter(s -> assignmentId.equals(s.getAssignmentId()) && status == s.getStatus())
                .toList();
    }

    public long countByAssignmentId(String assignmentId) {
        return table.scan().items().stream()
                .filter(s -> assignmentId.equals(s.getAssignmentId()))
                .count();
    }
}
