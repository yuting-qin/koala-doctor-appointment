package com.koala.koala_doctor_appointment.common;

public class EntityNotFoundException extends RuntimeException {

    private final String entityType;
    private final String entityId;

    public EntityNotFoundException(String entityType, String entityId) {
        super(entityType + " " + entityId + " not found");
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String entityType() {
        return entityType;
    }

    public String entityId() {
        return entityId;
    }
}
