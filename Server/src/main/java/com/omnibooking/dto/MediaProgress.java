package com.omnibooking.dto;

/**
 * DTO representing the current state of media progress for a property.
 * All values are computed by the backend (Redis Lua scripts) —
 * the frontend only renders this data.
 */
public record MediaProgress(
      int total,
      int queued,
      int processed,
      int failed,
      String status,
      int percentage,
      long lastUpdatedAt
) {}
