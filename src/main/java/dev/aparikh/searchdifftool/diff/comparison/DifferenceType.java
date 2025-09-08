package dev.aparikh.searchdifftool.diff.comparison;

/**
 * Enum representing the type of difference found between primary and secondary values.
 */
public enum DifferenceType {
    MISSING_IN_PRIMARY,
    MISSING_IN_SECONDARY, 
    TYPE_MISMATCH,
    DIFFERENT
}