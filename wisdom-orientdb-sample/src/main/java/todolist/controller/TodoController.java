package todolist.controller;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import javassist.util.proxy.Proxy;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.*;
import org.wisdom.api.annotations.scheduler.Async;
import org.wisdom.api.content.Json;
import org.wisdom.api.http.AsyncResult;
import org.wisdom.api.http.Result;
import org.wisdom.orientdb.object.OrientDbCrud;
import todolist.model.Todo;
import todolist.model.TodoList;

import javax.validation.Valid;
import java.util.Iterator;

import static org.wisdom.api.http.HttpMethod.*;

/**
 * Simple TodoList rest api.
 *
 * @version 1.0
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
@Controller
@Path("/list")
public class TodoController extends DefaultController{
    static {Class workaround = Proxy.class;}

    @Requires
    Json json;

    @Model(value = TodoList.class)
    private volatile OrientDbCrud<TodoList,String> listCrud;

    @Model(value = Todo.class)
    private volatile OrientDbCrud<Todo,String> todoCrud;

    @Validate
    private void start(){
        //Populate the db with some default value
        if(!listCrud.findAll().iterator().hasNext()){
            Todo todo = new Todo();
            todo.setContent("Check out this awesome todo demo!");
            todo.setDone(true);


            TodoList list = new TodoList();
            list.setName("Todo-List");
            list.setTodos(Lists.newArrayList(todo));
            listCrud.save(list);
        }

        listCrud.release();
    }

    /**
     * Return the list of todolist.
     *
     * @return list of todolist.
     */
    @Route(method = GET,uri = "")
    @Async
    public Result getList(){
        return ok(Iterables.toArray(listCrud.findAll(), TodoList.class)).json();
    }

    /**
     * Create a new todolist.
     *
     * @param list
     * @return the newly created todolist.
     */
    @Route(method = PUT, uri = "")
    @Async
    public Result putList(@Body TodoList list){
        return ok(listCrud.save(list)).json();
    }

    /**
     * Delete the todolist of given id.
     *
     * @param id of the todolist to remove.
     * @return 200 if removed, 404 if list with given id does not exist.
     */
    @Route(method = DELETE,uri = "/{id}")
    public Result delList(final @Parameter("id") String id){
        TodoList todoList = listCrud.findOne(id);

        if(todoList == null){
            return notFound();
        }

        listCrud.delete(todoList);

        return ok();
    }

    /**
     * Return the todolist of given id, 404 otherwise.
     *
     * @param id list id
     * @return the todolist of given id.
     */
    @Route(method = GET,uri = "/{id}")
    public Result getTodos(final @Parameter("id") String id){
        return new AsyncResult(()->{
            TodoList todoList = null;

            todoList = listCrud.findOne(id);

            if(todoList == null){
                return notFound();
            }

            String todos = json.mapper().writeValueAsString(todoList);
            listCrud.release();

            return ok(todos).json();
        });
    }

    /**
     * Create a new todo.
     *
     * @body.sample { "content" : "Get the milk", "done" : "true" }
     */
    @Route(method = PUT,uri = "/{id}")
    public Result createTodo(final @Parameter("id") String id,@Valid @Body Todo todo){
        return new AsyncResult(()->{
            TodoList todoList = null;
            todoList = listCrud.findOne(id);

            if(todoList == null){
                return notFound();
            }

            if(todo == null){
                return badRequest("Cannot create todo, content is null.");
            }

            todoList.getTodos().add(todo);

            todoList = listCrud.save(todoList);

            //convert into json object here, to be sure that the db is call in the same thread (through the proxy).
            String last =   json.mapper().writeValueAsString(Iterables.getLast(todoList.getTodos()));

            return ok(last).json();
        });
    }

    @Route(method = POST,uri = "/{id}/{todoId}")
    @Async
    public Result updateTodo(@Parameter("id") String listId,@Parameter("todoId") String todoId,@Valid @Body Todo todo){
        TodoList todoList = listCrud.findOne(listId);

        if(todoList == null){
            return notFound();
        }

        if(todo == null){
            return badRequest("The given todo is null");
        }

        if(!todoId.equals(todo.getId())){
            return badRequest("The id of the todo does not match the url one");
        }

        Iterator<Todo> itTodo = todoList.getTodos().iterator();
        while(itTodo.hasNext()){
            if(itTodo.next().getId().equals(todoId)){
                return ok(todoCrud.save(todo)).json();
            }
        }
        return notFound();
    }

    @Route(method = DELETE,uri = "/{id}/{todoId}")
    @Async
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
