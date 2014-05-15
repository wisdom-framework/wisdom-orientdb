/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package todolist.controller;

import com.google.common.collect.Iterables;
import org.apache.felix.ipojo.annotations.Validate;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.*;
import org.wisdom.api.http.Result;
import org.wisdom.api.model.Crud;
import todolist.model.Todo;
import todolist.model.TodoList;

import java.util.Iterator;

import static org.wisdom.api.http.HttpMethod.*;

/**
 * created: 5/13/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
@Controller
@Path("/list")
public class TodoController extends DefaultController{
    @Model(value = TodoList.class)
    private Crud<TodoList,String> listCrud;

    @Validate
    private void start(){
        if(!listCrud.findAll().iterator().hasNext()){
            Todo todo = new Todo();
            todo.setContent("Check out this awesome todo demo!");
            todo.setDone(true);


            TodoList list = new TodoList();
            list.setName("Home");
            list.addTodo(todo);
            listCrud.save(list);
        }
    }

    @Route(method = GET,uri = "/")
    public Result getList(){
        return ok(Iterables.toArray(listCrud.findAll(), TodoList.class)).json();
    }

    @Route(method = PUT, uri = "/")
    public Result putList(@Body TodoList list){
        return ok(listCrud.save(list)).json();
    }

    @Route(method = DELETE,uri = "/{id}")
    public Result delList(final @Parameter("id") String id){
        TodoList todoList = listCrud.findOne(id);

        if(todoList == null){
            return notFound();
        }

        listCrud.delete(todoList);

        return ok();
    }

    @Route(method = GET,uri = "/{id}")
    public Result getTodos(final @Parameter("id") String id){
        TodoList todoList = listCrud.findOne(id);

        if(todoList == null){
            return notFound();
        }

        return ok(todoList.getTodos()).json();
    }

    @Route(method = PUT,uri = "/{id}")
    public Result createTodo(final @Parameter("id") String id,@Body Todo todo){
        TodoList todoList = listCrud.findOne(id);

        if(todoList == null){
            return notFound();
        }

        todoList.addTodo(todo);
        todoList = listCrud.save(todoList);

        return ok(todoList.getTodos().get(todoList.getTodos().size())).json();
    }

    @Route(method = DELETE,uri = "/{id}/{todoId}")
    public Result delTodo(@Parameter("id") String listId,@Parameter("todoId") String todoId){
        TodoList todoList = listCrud.findOne(listId);

        if(todoList == null){
            return notFound();
        }

        Iterator<Todo> itTodo = todoList.getTodos().iterator();

        while(itTodo.hasNext()){
            if(itTodo.next().getId().equals(todoId)){
                itTodo.remove();
                listCrud.save(todoList);
                return ok();
            }
        }

        return notFound();
    }
}
