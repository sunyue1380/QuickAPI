package cn.schoolwow.quickapi.controller;

public class Response<T> {
    private boolean success;
    private T data;
    private String msg;

    public Response() {
    }

    public Response(boolean success, T data, String msg) {
        this.success = success;
        this.data = data;
        this.msg = msg;
    }
}
