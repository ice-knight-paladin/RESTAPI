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
    public static final String CHANGE_TASK_STATE_POSITION = "/api/task-states/{task_state_id}/position/change";

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

    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskStatePosition(@PathVariable(name = "task_state_id") Long taskSateId,
                                        @RequestParam(name = "left_task_state_id") Optional<Long> optionalLeftTaskStateId) {

        TaskStateEntity changeTaskState = getTaskStateOrThrowException(taskSateId);

        ProjectEntity project = changeTaskState.getProject();

        Optional<Long> optionalOldLeftTaskStateId = changeTaskState
                .getLeftTaskState()
                .map(TaskStateEntity::getId);

        if (optionalOldLeftTaskStateId.equals(optionalLeftTaskStateId)) {
            return taskStateDtoFactory.makeProjectDto(changeTaskState);
        }

        Optional<TaskStateEntity> optionalNewLeftTaskState = optionalLeftTaskStateId
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


        Optional<TaskStateEntity> optionalNewRightTaskState;
        if (!optionalNewLeftTaskState.isPresent()) {
            optionalNewRightTaskState = project.getTaskStates()
                    .stream()
                    .filter(anotherTaskState -> !anotherTaskState.getLeftTaskState().isPresent())
                    .findAny();
        } else {
            optionalNewRightTaskState = optionalNewLeftTaskState
                    .get()
                    .getRightTaskState();
        }

        Optional<TaskStateEntity> optionalOldLeftTaskState = changeTaskState.getLeftTaskState();
        Optional<TaskStateEntity> optionalOldRightTaskState = changeTaskState.getRightTaskState();

        optionalOldLeftTaskState
                .ifPresent(it -> {
                    it.setRightTaskState(optionalOldRightTaskState.orElse(null));

                    taskStateRepository.saveAndFlush(it);
                });

        optionalOldRightTaskState.ifPresent(it -> {
            it.setLeftTaskState(optionalOldLeftTaskState.orElse(null));

            taskStateRepository.saveAndFlush(it);
        });


        if (optionalNewLeftTaskState.isPresent()) {

            TaskStateEntity newLeftTaskEntity = optionalNewLeftTaskState.get();

            newLeftTaskEntity.setRightTaskState(changeTaskState);
            changeTaskState.setLeftTaskState(newLeftTaskEntity);
        } else {
            changeTaskState.setLeftTaskState(null);
        }

        if (optionalNewRightTaskState.isPresent()) {

            TaskStateEntity newRightTaskEntity = optionalNewRightTaskState.get();

            newRightTaskEntity.setLeftTaskState(changeTaskState);
            changeTaskState.setRightTaskState(newRightTaskEntity);
        } else {
            changeTaskState.setRightTaskState(null);
        }

        changeTaskState = taskStateRepository.saveAndFlush(changeTaskState);

        optionalNewRightTaskState
                .ifPresent(taskStateRepository::saveAndFlush);

        optionalNewLeftTaskState
                .ifPresent(taskStateRepository::saveAndFlush);

        return taskStateDtoFactory.makeProjectDto(changeTaskState);

    }

    private TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Task state with %s id doesnt exists", taskStateId))
                );
    }
}
