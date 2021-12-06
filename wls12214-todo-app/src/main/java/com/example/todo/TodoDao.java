package com.example.todo;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TodoDao {
    private static final String SELECT_ALL_TODO = "SELECT * FROM todo";
    private static final String FIND_BY_ID = "SELECT * FROM todo WHERE id = ?;";
    private static final String INSERT_TODO = "INSERT INTO todo  (title, completed) VALUES  (?, ?);";
    private static final String DELETE_TODO = "DELETE FROM todo WHERE id = ?;";
    private static final String UPDATE_TODO = "UPDATE todo SET title = ?,completed= ?, ordering =? where id = ?;";

    public TodoDao() {}

    protected Connection getConnection() {
        Connection connection = null;

        try {
            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:comp/env");
            DataSource ds = (DataSource) envContext.lookup("pg");
            connection = ds.getConnection();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            printSQLException(e);
        }

        return connection;
    }

    public Todo findById( Long id) {
        Todo todo = null;

        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_ID)) {
            preparedStatement.setLong(1, id);
            System.out.println(preparedStatement);
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                String title = rs.getString("title");
                Boolean completed = rs.getBoolean("completed");
                Integer ordering = rs.getInt("ordering");
                todo = new Todo(id, title, completed, ordering);
            }
        } catch (SQLException e) {
            printSQLException(e);
        }

        return todo;
    }

    public Todo addOne( Todo todo ) {
        Todo result = null;

        System.out.println(INSERT_TODO);
        // try-with-resource statement will auto close the connection.
        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(INSERT_TODO, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, todo.getTitle());
            preparedStatement.setBoolean(2, false);
            System.out.println(preparedStatement);
            preparedStatement.executeUpdate();

            ResultSet rs = preparedStatement.getGeneratedKeys();
            Long insertedId = 0L;

            if (rs.next()){
                insertedId = rs.getLong(1);
                result = this.findById( insertedId );
            }
        } catch (SQLException e) {
            printSQLException(e);
        }

        return result;
    }

    public List<Todo> selectAllTodo() {
        // using try-with-resources to avoid closing resources (boiler plate code)
        List<Todo> todos = new ArrayList<>();
        // Step 1: Establishing a Connection
        try (Connection connection = getConnection();
             // Step 2:Create a statement using connection object
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_TODO);) {
            System.out.println(preparedStatement);
            // Step 3: Execute the query or update query
            ResultSet rs = preparedStatement.executeQuery();

            // Step 4: Process the ResultSet object.
            while (rs.next()) {
                Long id = rs.getLong("id");
                String title = rs.getString("title");
                Boolean completed = rs.getBoolean("completed");
                Integer ordering = rs.getInt("ordering");
                todos.add(new Todo(id, title, completed, ordering));
            }
        } catch (SQLException e) {
            printSQLException(e);
        }
        return todos;
    }

    public boolean deleteOne(Long id)  {
        boolean rowDeleted = false;

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(DELETE_TODO);) {
            statement.setLong(1, id);
            rowDeleted = statement.executeUpdate() > 0;
        } catch (SQLException e) {
            printSQLException(e);
        }
        return rowDeleted;
    }

    public boolean updateOne(Todo todo) {
        boolean rowUpdated=false;

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(UPDATE_TODO);) {
            statement.setString(1, todo.getTitle());
            statement.setBoolean(2, todo.isCompleted());
            statement.setInt(3, todo.getOrdering());
            statement.setLong(4, todo.getId());

            rowUpdated = statement.executeUpdate() > 0;
        } catch (SQLException e) {
            printSQLException(e);
        }
        return rowUpdated;
    }

    private void printSQLException(SQLException ex) {
        for (Throwable e: ex) {
            if (e instanceof SQLException) {
                e.printStackTrace(System.err);
                System.err.println("SQLState: " + ((SQLException) e).getSQLState());
                System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
                System.err.println("Message: " + e.getMessage());
                Throwable t = ex.getCause();
                while (t != null) {
                    System.out.println("Cause: " + t);
                    t = t.getCause();
                }
            }
        }
    }
}
