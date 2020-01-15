package cn.schoolwow.quickapi;

import org.junit.Test;

public class QuickAPITest {

    @Test
    public void build() {
        QuickAPI.newInstance()
                .controller("cn.schoolwow.quickapi.controller")
                .entity("cn.schoolwow.quickapi.entity")
                .directory("./src/main/webapps")
                .url("/doc")
                .generate()
                .upload("http://127.0.0.1:9000");
    }
}
