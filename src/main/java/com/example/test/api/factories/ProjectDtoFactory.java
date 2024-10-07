package com.example.test.api.factories;

import com.example.test.api.dto.ProjectDto;
import com.example.test.store.entities.ProjectEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
public class ProjectDtoFactory {
    public ProjectDto makeProjectDto(ProjectEntity entity){
        return ProjectDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createAt(entity.getCreateAt())
                .build();
    }
}
