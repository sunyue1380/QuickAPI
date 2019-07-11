package cn.schoolwow.quickapi.util;

import cn.schoolwow.quickapi.QuickAPI;
import cn.schoolwow.quickapi.domain.APIController;
import com.alibaba.fastjson.JSON;
import com.sun.javadoc.ClassDoc;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;

public class JavaDocReaderTest {

    @Test
    public void extractJavaDoc() throws Exception {
        QuickAPIConfig.packageNames.add("cn.schoolwow.quickapi.controller");
        List<Class> classList = ReflectionUtil.scanPackageList();
        List<APIController> apiControllerList = ReflectionUtil.getAPIList(classList);
        String s = JSON.toJSONString(apiControllerList);
        System.out.println(s);
        File file = new File("./src/main/webapp/quickapi/api.json");
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        PrintWriter pw = new PrintWriter(file);
        pw.print(s);
        pw.flush();
        pw.close();
    }
}