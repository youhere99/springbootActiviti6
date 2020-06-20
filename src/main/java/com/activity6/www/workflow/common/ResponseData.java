package com.activity6.www.workflow.common;

import java.io.Serializable;


public class ResponseData<T> extends  BaseResponse implements Serializable {

    T data;

    public ResponseData() {
    }

    public ResponseData(int status, String message, T data) {
        super(status, message);
        this.data = data;
    }

    public ResponseData data(T data) {
        this.setData(data);
        return this;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static ResponseData ok(Object data) {
        return (new ResponseData()).data(data);
    }

    public static ResponseData ok() {
        return new ResponseData();
    }
}
