package com.example.test.api.controllers;

import com.example.test.api.dto.AskDto;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@RestController
public class ProjectController {

    ProjectDtoFactory projectDtoFactory;
    ProjectRepository projectRepository;


    public static final String FETCH_PROJECTS = "/api/projects";
    public static final String CREATE_PROJECT = "/api/projects";
    public static final String EDIT_PROJECT = "/api/projects/{project_id}";

    public static final String DELETE_PROJECT = "/api/projects/{project_id}";


    public static final String CREATE_OR_UPDATE_PROJECT = "/api/projects";

    @DeleteMapping(DELETE_PROJECT)
    public AskDto deleteProject(@PathVariable("project_id") Long projectId) {

        getProjectOrThrowException(projectId);

        projectRepository.deleteById(projectId);

        return AskDto.makeDefault(true);
    }

    @GetMapping(FETCH_PROJECTS)
    public List<ProjectDto> fetchProjects(@RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName) {

        optionalPrefixName = optionalPrefixName.filter(prefixName -> !prefixName.trim().isEmpty());

        Stream<ProjectEntity> projectStream = optionalPrefixName
                .map(projectRepository::streamAllByNameStartsWithIgnoreCase)
                .orElseGet(projectRepository::streamAll);


        return projectStream.map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_PROJECT)
    public ProjectDto createProject(@RequestParam("project_name") String projectName) {
        if (projectName.trim().isEmpty()) {
            throw new BadRequestException("Name cant be empty");
        }

        projectRepository
                .findByName(projectName)
                .ifPresent(prj -> {
                    throw new BadRequestException(String.format("Project \"%s\" already exists.", projectName));
                });

        ProjectEntity project = projectRepository.saveAndFlush(
                ProjectEntity.builder()
                        .name(projectName)
                        .build()
        );
        return projectDtoFactory.makeProjectDto(project);
    }

    @PutMapping(CREATE_OR_UPDATE_PROJECT)
    public ProjectDto createOrUpdateProject(
            @RequestParam(value = "project_id", required = false) Optional<Long> optionalProjectId,
            @RequestParam(value = "project_name", required = false) Optional<String> optionalProjectName) {

        optionalProjectName = optionalProjectName.filter(projectName -> !projectName.trim().isEmpty());

        final ProjectEntity project = optionalProjectId
                .map(this::getProjectOrThrowException)
                .orElseGet(() -> ProjectEntity.builder().build());

        boolean isCreate = !optionalProjectId.isPresent();

        if (isCreate && !optionalProjectName.isPresent()) {
            throw new BadRequestException("Project name cant be empty.");
        }

        optionalProjectName
                .ifPresent(projectName -> {

                    projectRepository
                            .findByName(projectName)
                            .filter(anotherProject -> !Objects.equals(anotherProject.getId(), project.getId()))
                            .ifPresent(anotherProject -> {
                                throw new BadRequestException(String.format("Project \"%s\" already exists.", projectName));
                            });

                    project.setName(projectName);

                });

        final ProjectEntity saveProject = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(saveProject);
    }

    @PatchMapping(EDIT_PROJECT)
    public ProjectDto editProject(
            @PathVariable("project_id") Long projectId,
            @RequestParam("project_name") String projectName) {

        if (projectName.trim().isEmpty()) {
            throw new BadRequestException("Name cant be empty");
        }

        ProjectEntity project = getProjectOrThrowException(projectId);

        projectRepository
                .findByName(projectName)
                .filter(anotherProject -> !Objects.equals(anotherProject.getId(), projectId))
                .ifPresent(anotherProject -> {
                    throw new BadRequestException(String.format("Project \"%s\" already exists.", projectName));
                });

        project.setName(projectName);

        project = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(project);
    }

    private ProjectEntity getProjectOrThrowException(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException(String.format("Project with \"%s\" doesnt exists.", projectId)));
    }


}
