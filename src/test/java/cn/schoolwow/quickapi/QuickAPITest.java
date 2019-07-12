package cn.schoolwow.quickapi;

import org.junit.Test;

public class QuickAPITest {

    @Test
    public void build() {
        QuickAPI.newInstance()
                .controller("cn.schoolwow.quickapi.controller")
                .entity("cn.schoolwow.quickapi.entity")
                .generate();
    }
}