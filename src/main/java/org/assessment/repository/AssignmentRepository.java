package org.assessment.repository;

import org.assessment.entity.Assignment;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Optional;

@Repository
public class AssignmentRepository {

    private final DynamoDbTable<Assignment> table;

    public AssignmentRepository(DynamoDbEnhancedClient enhancedClient) {
        // Table name "assignments" and partition key "assignmentId"
        // are defined directly in Assignment entity via @DynamoDbTableName and @DynamoDbPartitionKey
        this.table = enhancedClient.table("assignments", TableSchema.fromBean(Assignment.class));
    }

    public Assignment save(Assignment assignment) {
        table.putItem(assignment);
        return assignment;
    }

    public Optional<Assignment> findById(String assignmentId) {
        return Optional.ofNullable(table.getItem(Key.builder().partitionValue(assignmentId).build()));
    }

    public List<Assignment> findAll() {
        return table.scan().items().stream().toList();
    }

    public void deleteById(String assignmentId) {
        table.deleteItem(Key.builder().partitionValue(assignmentId).build());
    }

    public List<Assignment> findByCourseId(String courseId) {
        return table.scan().items().stream()
                .filter(a -> courseId.equals(a.getCourseId()))
                .toList();
    }

    public List<Assignment> findByCreatedBy(String createdBy) {
        return table.scan().items().stream()
                .filter(a -> createdBy.equals(a.getCreatedBy()))
                .toList();
    }
}
