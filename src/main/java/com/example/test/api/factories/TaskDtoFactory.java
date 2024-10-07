package com.example.test.api.factories;

import com.example.test.api.dto.TaskDto;
import com.example.test.api.dto.TaskStateDto;
import com.example.test.store.entities.TaskEntity;
import com.example.test.store.entities.TaskStateEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskDtoFactory {
    public TaskDto makeProjectDto(TaskEntity entity) {
        return TaskDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createAt(entity.getCreateAt())
                .description(entity.getDescription())
                .build();
    }
}
