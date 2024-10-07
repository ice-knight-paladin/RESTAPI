package com.example.test.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskDto {
    @NonNull
    Long id;

    @NonNull
    String name;

    @NonNull
    @JsonProperty("create_at")
    Instant createAt;

    @NonNull
    String description;
}
