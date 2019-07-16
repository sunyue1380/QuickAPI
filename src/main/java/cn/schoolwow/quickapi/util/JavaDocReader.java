package cn.schoolwow.quickapi.util;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaDocReader {
    private static Logger logger = LoggerFactory.getLogger(JavaDocReader.class);
    private static List<ClassDoc> classDocList = new ArrayList<>();
    public static class Doclet {
        public Doclet() {
        }
        public static boolean start(RootDoc root) {
            classDocList.addAll(Arrays.asList(root.classes()));
            return true;
        }
    }
    private static String classPath = null;
    static{
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URL[] urls = urlClassLoader.getURLs();
        StringBuilder classPathBuilder = new StringBuilder();
        for(URL url:urls){
            classPathBuilder.append(url.getPath().substring(1)+";");
        }
        classPath = classPathBuilder.toString();
    }

    public static ClassDoc[] getControllerJavaDoc(){
        classDocList.clear();
        for(String packageName:QuickAPIConfig.controllerPackageNameList){
            getJavaDoc(classPath,packageName);
        }
        return classDocList.toArray(new ClassDoc[0]);
    }

    public static ClassDoc[] getEntityJavaDoc() {
        classDocList.clear();
        for(String packageName:QuickAPIConfig.entityPackageNameList){
            getJavaDoc(classPath,packageName);
        }
        return classDocList.toArray(new ClassDoc[0]);
    }

    private static void getJavaDoc(String classPath,String packageName){
        String[] commands = {"-doclet",
                Doclet.class.getName(),
                "-encoding","utf-8",
                "-private",
                "-quiet",
                "-classpath",
                classPath,
                "-sourcepath",
                System.getProperty("user.dir")+"/src/main/java",
                "-subpackages",
                packageName};
        com.sun.tools.javadoc.Main.execute(commands);
    }
}
