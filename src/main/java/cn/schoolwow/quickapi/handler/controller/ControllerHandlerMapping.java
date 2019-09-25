package cn.schoolwow.quickapi.handler.controller;

public enum ControllerHandlerMapping {
    QuickServer("cn.schoolwow.quickserver.request.RequestMeta",QuickServerControllerHandler.class),
    Swagger("org.springframework.web.bind.annotation.RequestMapping",SpringMVCControllerHandler.class);
    public String className;
    public Class _class;

    ControllerHandlerMapping(String className, Class _class) {
        this.className = className;
        this._class = _class;
    }
}
