package dev.aparikh.searchdifftool.api.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchDiffExceptionTest {

    @Test
    void createException_WithMessage_SetsMessage() {
        // Given
        String message = "Test error message";

        // When
        SearchDiffException exception = new SearchDiffException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void createException_WithMessageAndCause_SetsBoth() {
        // Given
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");

        // When
        SearchDiffException exception = new SearchDiffException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exception_IsRuntimeException() {
        // When
        SearchDiffException exception = new SearchDiffException("test");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}