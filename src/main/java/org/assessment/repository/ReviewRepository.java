package org.assessment.repository;

import org.assessment.entity.Review;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Optional;

@Repository
public class ReviewRepository {

    private final DynamoDbTable<Review> table;

    public ReviewRepository(DynamoDbEnhancedClient enhancedClient) {
        // Table name "reviews" and partition key "reviewId"
        // are defined directly in Review entity via @DynamoDbTableName and @DynamoDbPartitionKey
        this.table = enhancedClient.table("reviews", TableSchema.fromBean(Review.class));
    }

    public Review save(Review review) {
        table.putItem(review);
        return review;
    }

    public Optional<Review> findById(String reviewId) {
        return Optional.ofNullable(table.getItem(Key.builder().partitionValue(reviewId).build()));
    }

    public Optional<Review> findBySubmissionId(String submissionId) {
        return table.scan().items().stream()
                .filter(r -> submissionId.equals(r.getSubmissionId()))
                .findFirst();
    }

    public List<Review> findByReviewerId(String reviewerId) {
        return table.scan().items().stream()
                .filter(r -> reviewerId.equals(r.getReviewerId()))
                .toList();
    }
}
