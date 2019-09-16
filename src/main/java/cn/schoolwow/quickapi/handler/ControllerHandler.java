package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.API;
import cn.schoolwow.quickapi.domain.APIParameter;

import java.lang.reflect.Method;

public interface ControllerHandler {
    String getBaseUrl(Class _class);

    void handleRequestMapping(Method method, API api);

    APIParameter[] handleParameter(Method method, API api);
}
