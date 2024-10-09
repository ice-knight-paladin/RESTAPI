package com.example.test.api.controllers;


import com.example.test.api.controllers.helpers.ControllerHelper;
import com.example.test.api.dto.TaskStateDto;
import com.example.test.api.exceptions.BadRequestException;
import com.example.test.api.exceptions.NotFoundException;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public static final String UPDATE_TASK_STATE = "/api/task-states/{task_state_id}";
    public static final String CHANGE_TASK_POSITION = "/api/task-states/{task_state_id}/position/change";

    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(@PathVariable(name = "project_id") Long projectId) {

        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        return project.getTaskStates().stream().map(taskStateDtoFactory::makeProjectDto).collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskState(@PathVariable(name = "project_id") Long projectId, @RequestParam(name = "task_state_name") String taskStateName) {

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task sate name cant be empty");
        }

        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        Optional<TaskStateEntity> optionalAnotherTaskState = Optional.empty();

        for (TaskStateEntity taskState : project.getTaskStates()) {
            if (taskState.getName().equalsIgnoreCase(taskStateName)) {
                throw new BadRequestException(String.format("Task state \"%s\" already exists", taskStateName));
            }

            if (!taskState.getRightTaskState().isPresent()) {
                optionalAnotherTaskState = Optional.of(taskState);
                break;
            }
        }


        TaskStateEntity taskState = taskStateRepository.saveAndFlush(TaskStateEntity.builder().name(taskStateName).project(project).build());

        optionalAnotherTaskState.ifPresent(anotherTaskState -> {

            taskState.setLeftTaskState(anotherTaskState);
            anotherTaskState.setRightTaskState(taskState);
            taskStateRepository.saveAndFlush(anotherTaskState);

        });
        final TaskStateEntity savedTaskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeProjectDto(savedTaskState);

    }

    @PatchMapping(UPDATE_TASK_STATE)
    public TaskStateDto updateTaskState(@PathVariable(name = "task_state_id") Long taskSateId,
                                        @RequestParam(name = "task_state_name") String taskStateName) {

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task sate name cant be empty");
        }

        TaskStateEntity taskState = getTaskStateOrThrowException(taskSateId);

        taskStateRepository
                .findTaskStateEntityByProjectIdAndNameContainingIgnoreCase(taskState.getProject().getId(), taskStateName)
                .filter(anotherTaskState -> !anotherTaskState.getId().equals(taskSateId))
                .ifPresent(anotherTaskState -> {
                    throw new BadRequestException(String.format("Task state %s already exists.", taskStateName));
                });

        taskState.setName(taskStateName);
        taskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeProjectDto(taskState);

    }

    @PatchMapping(CHANGE_TASK_POSITION)
    public TaskStateDto changeTaskState(@PathVariable(name = "task_state_id") Long taskSateId,
                                        @RequestParam(name = "left_task_state_id") Optional<Long> optionalLeftTaskStateId) {

        TaskStateEntity changeTaskState = getTaskStateOrThrowException(taskSateId);

        ProjectEntity project = changeTaskState.getProject();

        Optional<Long> oldLeftTaskStateId = changeTaskState
                .getLeftTaskState()
                .map(TaskStateEntity::getId);

        if (oldLeftTaskStateId.equals(optionalLeftTaskStateId)) {
            return taskStateDtoFactory.makeProjectDto(changeTaskState);
        }

        Optional<TaskStateEntity> newLeftTaskState = optionalLeftTaskStateId
                .map(leftTaskStateId -> {
                    if (taskSateId.equals(leftTaskStateId)) {
                        throw new BadRequestException("Left task state id equals changed state id.");
                    }

                    TaskStateEntity leftTaskStateEntity = getTaskStateOrThrowException(leftTaskStateId);

                    if (!project.getId().equals(leftTaskStateEntity.getProject().getId())) {
                        throw new BadRequestException("Task state position can be changed in within the same project.");
                    }

                    return leftTaskStateEntity;
                });


        Optional<TaskStateEntity> newRightTaskState;
        if (!newLeftTaskState.isPresent()) {
            newRightTaskState = project.getTaskStates()
                    .stream()
                    .filter(anotherTaskState -> Objects.isNull(anotherTaskState.getLeftTaskState()))
                    .findAny();
        } else {
            newRightTaskState = newLeftTaskState
                    .get()
                    .getRightTaskState();
        }


        changeTaskState.setName(taskStateName);
        changeTaskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeProjectDto(taskState);

    }

    private TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Task state with %s id doesnt exists", taskStateId))
                );
    }
}
