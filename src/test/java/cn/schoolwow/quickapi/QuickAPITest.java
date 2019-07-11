package cn.schoolwow.quickapi;

import org.junit.Test;

public class QuickAPITest {

    @Test
    public void build() {
        QuickAPI.newInstance()
                .scan("cn.schoolwow.quickapi.controller")
                .url("/docs")
                .generate();
    }
}