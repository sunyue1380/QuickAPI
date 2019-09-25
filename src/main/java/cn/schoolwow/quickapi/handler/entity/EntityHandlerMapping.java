package cn.schoolwow.quickapi.handler.entity;

public enum EntityHandlerMapping {
    QuickServer("cn.schoolwow.quickdao.dao.DAO",QuickDAOEntityHandler.class),
    Swagger("io.swagger.annotations.ApiModel",SwaggerEntityHandler.class);
    public String className;
    public Class _class;

    EntityHandlerMapping(String className, Class _class) {
        this.className = className;
        this._class = _class;
    }
}
