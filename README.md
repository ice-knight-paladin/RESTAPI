Project
- id (long)
- name (string)
- createAt (time)
- taskStates (TaskState[] array)

TaskState
- id (long)
- name (string)
- ordinal (long)
- createdAt (time)
- tasks (Task[] array)

Task
- id (long)
- name (string)
- description (string)
- createdAt (time)


DB Entities  
DB Entity Managment   
REST API 

ProjectController
- Создать проекты
- Редактировать проект
- Удалять проект


Post- отвечает за создание чего-либо/запуск какой-то логики  
Get - отвечает за получение какой-то информации  
Put - отвечает за полную замену
Patch - отвечает за обновление объекта  
Delete - отвечает за удаление объекта  