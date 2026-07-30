package org.assessment.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Enum Tests")
class EnumsTest {

    @Nested
    @DisplayName("AssignmentStatus")
    class AssignmentStatusTest {

        @Test
        @DisplayName("should have exactly 3 values: DRAFT, PUBLISHED, CLOSED")
        void hasExpectedValues() {
            AssignmentStatus[] values = AssignmentStatus.values();
            assertThat(values)
                    .hasSize(3)
                    .containsExactly(
                            AssignmentStatus.DRAFT,
                            AssignmentStatus.PUBLISHED,
                            AssignmentStatus.CLOSED);
        }

        @Test
        @DisplayName("should parse from string via valueOf")
        void valueOf_works() {
            assertThat(AssignmentStatus.valueOf("DRAFT")).isEqualTo(AssignmentStatus.DRAFT);
            assertThat(AssignmentStatus.valueOf("PUBLISHED")).isEqualTo(AssignmentStatus.PUBLISHED);
            assertThat(AssignmentStatus.valueOf("CLOSED")).isEqualTo(AssignmentStatus.CLOSED);
        }

        @Test
        @DisplayName("should throw on invalid valueOf")
        void valueOf_invalid_throws() {
            assertThatThrownBy(() -> AssignmentStatus.valueOf("UNKNOWN"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("name() should return the enum constant name as string")
        void name_returnsString() {
            assertThat(AssignmentStatus.PUBLISHED.name()).isEqualTo("PUBLISHED");
        }
    }

    @Nested
    @DisplayName("AssignmentType")
    class AssignmentTypeTest {

        @Test
        @DisplayName("should have exactly 3 values: FILE_UPLOAD, QUIZ, PROJECT")
        void hasExpectedValues() {
            AssignmentType[] values = AssignmentType.values();
            assertThat(values)
                    .hasSize(3)
                    .containsExactly(
                            AssignmentType.FILE_UPLOAD,
                            AssignmentType.QUIZ,
                            AssignmentType.PROJECT);
        }

        @Test
        @DisplayName("should parse from string via valueOf")
        void valueOf_works() {
            assertThat(AssignmentType.valueOf("FILE_UPLOAD")).isEqualTo(AssignmentType.FILE_UPLOAD);
            assertThat(AssignmentType.valueOf("QUIZ")).isEqualTo(AssignmentType.QUIZ);
            assertThat(AssignmentType.valueOf("PROJECT")).isEqualTo(AssignmentType.PROJECT);
        }
    }

    @Nested
    @DisplayName("DifficultyLevel")
    class DifficultyLevelTest {

        @Test
        @DisplayName("should have exactly 3 values: BEGINNER, INTERMEDIATE, ADVANCED")
        void hasExpectedValues() {
            DifficultyLevel[] values = DifficultyLevel.values();
            assertThat(values)
                    .hasSize(3)
                    .containsExactly(
                            DifficultyLevel.BEGINNER,
                            DifficultyLevel.INTERMEDIATE,
                            DifficultyLevel.ADVANCED);
        }

        @Test
        @DisplayName("ordinal order should be BEGINNER < INTERMEDIATE < ADVANCED")
        void ordinalOrder() {
            assertThat(DifficultyLevel.BEGINNER.ordinal())
                    .isLessThan(DifficultyLevel.INTERMEDIATE.ordinal())
                    .isLessThan(DifficultyLevel.ADVANCED.ordinal());
        }
    }

    @Nested
    @DisplayName("ResultStatus")
    class ResultStatusTest {

        @Test
        @DisplayName("should have exactly 3 values: PASS, FAIL, PENDING")
        void hasExpectedValues() {
            ResultStatus[] values = ResultStatus.values();
            assertThat(values)
                    .hasSize(3)
                    .containsExactly(
                            ResultStatus.PASS,
                            ResultStatus.FAIL,
                            ResultStatus.PENDING);
        }

        @Test
        @DisplayName("should parse from string via valueOf")
        void valueOf_works() {
            assertThat(ResultStatus.valueOf("PASS")).isEqualTo(ResultStatus.PASS);
            assertThat(ResultStatus.valueOf("FAIL")).isEqualTo(ResultStatus.FAIL);
            assertThat(ResultStatus.valueOf("PENDING")).isEqualTo(ResultStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Role")
    class RoleTest {

        @Test
        @DisplayName("should have exactly 3 values: ADMIN, TRAINER, LEARNER")
        void hasExpectedValues() {
            Role[] values = Role.values();
            assertThat(values)
                    .hasSize(3)
                    .containsExactly(
                            Role.ADMIN,
                            Role.TRAINER,
                            Role.LEARNER);
        }

        @Test
        @DisplayName("should parse from string via valueOf")
        void valueOf_works() {
            assertThat(Role.valueOf("ADMIN")).isEqualTo(Role.ADMIN);
            assertThat(Role.valueOf("LEARNER")).isEqualTo(Role.LEARNER);
        }
    }

    @Nested
    @DisplayName("SubmissionStatus")
    class SubmissionStatusTest {

        @Test
        @DisplayName("should have exactly 4 values")
        void hasExpectedValues() {
            SubmissionStatus[] values = SubmissionStatus.values();
            assertThat(values)
                    .hasSize(4)
                    .containsExactly(
                            SubmissionStatus.NOT_SUBMITTED,
                            SubmissionStatus.SUBMITTED,
                            SubmissionStatus.UNDER_REVIEW,
                            SubmissionStatus.REVIEWED);
        }

        @Test
        @DisplayName("ordinal of NOT_SUBMITTED should be 0")
        void notSubmitted_ordinalIsZero() {
            assertThat(SubmissionStatus.NOT_SUBMITTED.ordinal()).isZero();
        }

        @Test
        @DisplayName("REVIEWED should be last (ordinal 3)")
        void reviewed_ordinalIsThree() {
            assertThat(SubmissionStatus.REVIEWED.ordinal()).isEqualTo(3);
        }
    }
}
