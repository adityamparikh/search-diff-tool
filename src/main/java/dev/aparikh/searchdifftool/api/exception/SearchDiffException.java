package dev.aparikh.searchdifftool.api.exception;

public class SearchDiffException extends RuntimeException {
    
    public SearchDiffException(String message) {
        super(message);
    }
    
    public SearchDiffException(String message, Throwable cause) {
        super(message, cause);
    }
}