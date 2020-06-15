package cn.schoolwow.quickapi.handler.controller;

public enum ControllerHandlerMapping {
    QuickServer("cn.schoolwow.quickserver.request.RequestMeta",QuickServerControllerHandler.class),
    SpringMVC("org.springframework.web.bind.annotation.RequestMapping",SpringMVCControllerHandler.class);
    public String className;
    public Class clazz;

    ControllerHandlerMapping(String className, Class _class) {
        this.className = className;
        this.clazz = _class;
    }
}
