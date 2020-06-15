package cn.schoolwow.quickapi;

import cn.schoolwow.quickapi.handler.controller.ControllerHandlerMapping;
import org.junit.Test;

public class QuickAPITest {

    @Test
    public void build() {
        QuickAPI.newInstance()
                .controllerHandlerMapping(ControllerHandlerMapping.SpringMVC)
                .controller("cn.schoolwow.quickapi.controller")
                .sourcePath(System.getProperty("user.dir")+"/src/test/java")
                .directory("./src/main/resources")
                .url("/doc")
                .generate();
    }
}
