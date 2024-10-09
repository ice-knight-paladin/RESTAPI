package com.example.test.api.controllers;


import com.example.test.api.controllers.helpers.ControllerHelper;
import com.example.test.api.dto.TaskStateDto;
import com.example.test.api.exceptions.BadRequestException;
import com.example.test.api.factories.TaskStateDtoFactory;
import com.example.test.store.entities.ProjectEntity;
import com.example.test.store.entities.TaskStateEntity;
import com.example.test.store.repositories.TaskStateRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@RestController
public class TaskStateController {

    ControllerHelper controllerHelper;

    TaskStateRepository taskStateRepository;

    TaskStateDtoFactory taskStateDtoFactory;

    public static final String GET_TASK_STATES = "/api/projects/{project_id}/task-states";
    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/task-states";
    public static final String CREATE_OR_UPDATE_PROJECT = "/api/projects";

    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(@PathVariable(name = "project_id") Long projectId) {

        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        return project.getTaskStates().stream().map(taskStateDtoFactory::makeProjectDto).collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskStateDto(@PathVariable(name = "project_id") Long projectId, @RequestParam(name = "task_state_name") String taskStateName) {

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task sate name cant be empty");
        }

        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        project.getTaskStates().stream().map(TaskStateEntity::getName).filter(anotherTaskStateName -> anotherTaskStateName.equalsIgnoreCase(taskStateName)).findAny().ifPresent(it -> {
            throw new BadRequestException(String.format("Task state \"%s\" already exists", taskStateName));
        });

        TaskStateEntity taskState = taskStateRepository.saveAndFlush(TaskStateEntity.builder().name(taskStateName).build());

        taskStateRepository.findTaskStateEntityByRightTaskStateIdIsNullAndProjectId(projectId).ifPresent(anotherTaskState -> {

            taskState.setLeftTaskState(anotherTaskState);
            anotherTaskState.setRightTaskState(taskState);
            taskStateRepository.saveAndFlush(anotherTaskState);

        });
        final TaskStateEntity savedTaskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeProjectDto(savedTaskState);

    }
}
