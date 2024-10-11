package com.example.test.api.controllers;

import com.example.test.api.controllers.helpers.ControllerHelper;
import com.example.test.api.dto.AskDto;
import com.example.test.api.dto.TaskDto;
import com.example.test.api.dto.TaskStateDto;
import com.example.test.api.exceptions.BadRequestException;
import com.example.test.api.exceptions.NotFoundException;
import com.example.test.api.factories.TaskDtoFactory;
import com.example.test.store.entities.ProjectEntity;
import com.example.test.store.entities.TaskEntity;
import com.example.test.store.entities.TaskStateEntity;
import com.example.test.store.repositories.TaskRepository;
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
public class TaskController {

    ControllerHelper controllerHelper;

    TaskRepository taskRepository;
    TaskStateRepository taskStateRepository;
    TaskDtoFactory taskDtoFactory;

    public static final String GET_TASKS = "/api/task-states/{task_state_id}/tasks";

    public static final String CREATE_TASK = "/api/task-states/{task_state_id}/tasks";

    public static final String DELETE_TASK = "/api/task-states/{task_state_id}/tasks/{task_id}";

    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(@PathVariable(name = "task_state_id") Long taskStateId) {

        TaskStateEntity taskStateEntity = controllerHelper.getTaskStateEntityOrThrow(taskStateId);


        return taskStateEntity.getTasks().stream().map(taskDtoFactory::makeProjectDto).collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK)
    public TaskDto createTaskDto(@PathVariable(name = "task_state_id") Long taskStateId,
                                 @RequestParam(name = "task_name") String taskName,
                                 @RequestParam(name = "task_description") String taskDescription) {

        if (taskName.isBlank()) {
            throw new BadRequestException("Task name cant be empty");
        }

        TaskStateEntity taskState = controllerHelper.getTaskStateEntityOrThrow(taskStateId);

        taskState
                .getTasks()
                .stream()
                .filter(anotherTaskName -> Objects.equals(anotherTaskName.getName(), taskName))
                .findAny()
                .ifPresent(it -> {
                    throw new BadRequestException(String.format("Task %s already exists.", taskName));
                });

        TaskEntity taskEntity = taskRepository.saveAndFlush(TaskEntity.builder().name(taskName).description(taskDescription).build());

        taskState.getTasks().add(TaskEntity.builder().name(taskName).description(taskDescription).id(taskEntity.getId()).build());
        taskStateRepository.saveAndFlush(taskState);
        return taskDtoFactory.makeProjectDto(taskEntity);
    }

    @DeleteMapping(DELETE_TASK)
    public AskDto deleteTask(@PathVariable(name = "task_state_id") Long taskStateId,
                             @PathVariable(name = "task_id") Long taskId) {

        TaskEntity changeTask = taskRepository
                .findById(taskId)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Task with %s id doesnt exists", taskId))
                );

        TaskStateEntity taskState = controllerHelper.getTaskStateEntityOrThrow(taskStateId);
        taskState
                .getTasks()
                .remove(changeTask);

        taskStateRepository.saveAndFlush(taskState);
        taskRepository.delete(changeTask);


        return AskDto.builder().answer(true).build();
    }
}

