package com.example.eCommerceUdemy.exception;



public class ResourceNotFoundException extends RuntimeException {
    private String resourceName;
    private String fieldName;
    private Long fieldId;
    private String field;

    public ResourceNotFoundException() {
    }

    public ResourceNotFoundException(String field, String resourceName, Long fieldId) {
        super(String.format("Resource not found for field %s and resource name %s with %d", field, resourceName,fieldId));
        this.field = field;
        this.resourceName = resourceName;
        this.fieldId = fieldId;
    }

    public ResourceNotFoundException(String field, String resourceName, String fileName) {
        super(String.format("Resource not found for field %s and resource name %s with %s", field, resourceName));
        this.field = field;
        this.resourceName = resourceName;
        this.fieldName = fieldName;
    }
}
