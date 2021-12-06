package com.example.resource;

import com.example.todo.TodoDao;
import com.example.todo.Todo;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/todo")
public class TodoRestController {
    private TodoDao todoDao;

    public TodoRestController() {
        this.todoDao = new TodoDao();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Todo> getAll() {
        List<Todo> todos = this.todoDao.selectAllTodo();
        return todos;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addOne(Todo item) {
        Todo result = todoDao.addOne(item);
        if ( result != null ) {
            return Response.status(Response.Status.CREATED).entity(result).build();
        } else {
            return Response.status(Response.Status.NOT_MODIFIED).entity(result).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteOne(@PathParam("id") Long id) {
        todoDao.deleteOne(id);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response updateOne(@PathParam("id") Long id, Todo item) {
        Boolean result = todoDao.updateOne(item);

        if ( result ) {
            return Response.status(Response.Status.OK).entity(item).build();
        } else {
            return Response.status(Response.Status.NOT_MODIFIED).entity(item).build();
        }
    }
}
