package com.example.todo;

public class Todo {
    private Long id;
    private String title;
    private Boolean completed;
    private Integer ordering;

    public Todo() {
    }

    public Todo(Long id, String title, Boolean completed, Integer ordering) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.ordering = ordering;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Integer getOrdering() {
        return ordering;
    }

    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }
}