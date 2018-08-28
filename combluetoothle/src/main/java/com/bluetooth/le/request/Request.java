package com.bluetooth.le.request;

/**
 * Created by caoxuanphong on 1/3/18.
 */

public class Request {
    private RequestCmd cmd;
    private RequestData data;

    public Request(RequestCmd cmd, RequestData data) {
        this.cmd = cmd;
        this.data = data;
    }

    public RequestCmd getCmd() {
        return cmd;
    }

    public void setCmd(RequestCmd cmd) {
        this.cmd = cmd;
    }

    public RequestData getData() {
        return data;
    }

    public void setData(RequestData data) {
        this.data = data;
    }
}
