package com.example.test.api.controllers;

import com.example.test.api.controllers.helpers.ControllerHelper;
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
    ControllerHelper controllerHelper;


    public static final String FETCH_PROJECTS = "/api/projects";
    public static final String DELETE_PROJECT = "/api/projects/{project_id}";
    public static final String CREATE_OR_UPDATE_PROJECT = "/api/projects";

    @DeleteMapping(DELETE_PROJECT)
    public AskDto deleteProject(@PathVariable("project_id") Long projectId) {

        controllerHelper.getProjectOrThrowException(projectId);

        projectRepository.deleteById(projectId);

        return AskDto.makeDefault(true);
    }

    @GetMapping(FETCH_PROJECTS)
    public List<ProjectDto> fetchProjects(@RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName) {

        optionalPrefixName = optionalPrefixName.filter(prefixName -> !prefixName.trim().isEmpty());

        Stream<ProjectEntity> projectStream = optionalPrefixName.map(projectRepository::streamAllByNameStartsWithIgnoreCase).orElseGet(projectRepository::streamAllBy);


        return projectStream.map(projectDtoFactory::makeProjectDto).collect(Collectors.toList());
    }

    @PutMapping(CREATE_OR_UPDATE_PROJECT)
    public ProjectDto createOrUpdateProject(@RequestParam(value = "project_id", required = false) Optional<Long> optionalProjectId, @RequestParam(value = "project_name", required = false) Optional<String> optionalProjectName) {

        optionalProjectName = optionalProjectName.filter(projectName -> !projectName.trim().isEmpty());

        boolean isCreate = !optionalProjectId.isPresent();

        if (isCreate && !optionalProjectName.isPresent()) {
            throw new BadRequestException("Project name cant be empty.");
        }

        final ProjectEntity project = optionalProjectId.map(controllerHelper::getProjectOrThrowException).orElseGet(() -> ProjectEntity.builder().build());


        optionalProjectName.ifPresent(projectName -> {

            projectRepository.findByName(projectName).filter(anotherProject -> !Objects.equals(anotherProject.getId(), project.getId())).ifPresent(anotherProject -> {
                throw new BadRequestException(String.format("Project \"%s\" already exists.", projectName));
            });

            project.setName(projectName);

        });

        final ProjectEntity saveProject = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(saveProject);
    }


}
