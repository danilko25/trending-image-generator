package com.ai.generation.exception;

import java.util.UUID;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(Class<?> clazz, UUID id) {
        super(clazz.getSimpleName() + " with id: " + id + " not found!");
    }
}
