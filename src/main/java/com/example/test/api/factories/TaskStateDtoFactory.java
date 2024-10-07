package com.example.test.api.factories;

import com.example.test.api.dto.ProjectDto;
import com.example.test.api.dto.TaskStateDto;
import com.example.test.store.entities.ProjectEntity;
import com.example.test.store.entities.TaskStateEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
public class TaskStateDtoFactory {

    public TaskStateDto makeProjectDto(TaskStateEntity entity) {
        return TaskStateDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createAt(entity.getCreateAt())
                .ordinal(entity.getOrdinal())
                .build();
    }
}
