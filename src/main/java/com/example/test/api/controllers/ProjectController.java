package com.example.test.api.controllers;

import com.example.test.api.dto.ProjectDto;
import com.example.test.api.exceptions.NotFoundException;
import com.example.test.api.factories.ProjectDtoFactory;
import com.example.test.store.entities.ProjectEntity;
import com.example.test.store.repositories.ProjectRepository;
import com.example.test.api.exceptions.BadRequestException;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@RestController
public class ProjectController {

    ProjectDtoFactory projectDtoFactory;
    ProjectRepository projectRepository;


    public static final String FETCH_PROJECTS = "/api/p";
    public static final String CREATE_PROJECT = "/api/projects";
    public static final String EDIT_PROJECT = "/api/projects/{project_id}";


    @PostMapping(FETCH_PROJECTS)
    public List<ProjectDto> fetchProjects(@RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName) {

        optionalPrefixName = optionalPrefixName.filter(prefixName-> !prefixName.trim().isEmpty());

        Stream<ProjectEntity> projectStream;

        if (optionalPrefixName.isPresent()){
            projectStream = projectRepository.streamAllByNameStartsWithIgnoreCase(optionalPrefixName.get());
        } else {
            projectStream = projectRepository.streamAll();
        }




        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name cant be empty");
        }
        projectRepository
                .findByName(name)
                .ifPresent(prj -> {
                    throw new BadRequestException(String.format("Project \"%s\" already exists.", name));
                });

        ProjectEntity project = projectRepository.saveAndFlush(
                ProjectEntity.builder()
                        .name(name)
                        .build()
        );
        return projectDtoFactory.makeProjectDto(project);
    }

    @PostMapping(CREATE_PROJECT)
    public ProjectDto createProject(@RequestParam String name) {
        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name cant be empty");
        }
        projectRepository
                .findByName(name)
                .ifPresent(prj -> {
                    throw new BadRequestException(String.format("Project \"%s\" already exists.", name));
                });

        ProjectEntity project = projectRepository.saveAndFlush(
                ProjectEntity.builder()
                        .name(name)
                        .build()
        );
        return projectDtoFactory.makeProjectDto(project);
    }

    @PatchMapping(EDIT_PROJECT)
    public ProjectDto editPatch(
            @PathVariable("project_id") Long projectId,
            @RequestParam String name) {

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name cant be empty");
        }

        ProjectEntity project = projectRepository
                .findByName(name)
                .orElseThrow(() -> new NotFoundException(String.format("Project with \"%s\" doesnt exists.", projectId)));

        projectRepository
                .findByName(name)
                .filter(anotherProject -> !Objects.equals(anotherProject.getId(), projectId))
                .ifPresent(anotherProject -> {
                    throw new BadRequestException(String.format("Project \"%s\" already exists.", name));
                });

        project.setName(name);

        project = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(project);
    }


}
