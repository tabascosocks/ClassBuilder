package com.edumentic.classbuilder.model;

public enum Gender {
    MALE, FEMALE, NA;

    private static final Gender DEFAULT = NA;

    public static Gender fromString(String value) {
        if (value == null) {
            return DEFAULT;
        }
        switch (value.trim().toUpperCase()) {
            case "M":
                return MALE;
            case "F":
                return FEMALE;
            default:
                return DEFAULT;
        }
    }

    @Override
    public String toString() {
        return switch (this) {
            case MALE -> "Male";
            case FEMALE -> "Female";
            default -> "Not Applicable";
        };
    }
}
