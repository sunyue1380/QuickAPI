package cn.schoolwow.quickapi;

import org.junit.Test;

public class QuickAPITest {

    @Test
    public void build() {
        QuickAPI.newInstance()
                .controller("cn.schoolwow.quickapi.controller")
                .sourcePath(System.getProperty("user.dir")+"/src/test/java")
                .directory("./src/main/resources")
                .url("/doc")
                .generate();
    }
}
