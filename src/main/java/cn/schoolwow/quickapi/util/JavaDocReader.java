package cn.schoolwow.quickapi.util;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

import java.net.URL;
import java.net.URLClassLoader;

public class JavaDocReader {
    private static RootDoc root;
    public static class Doclet {
        public Doclet() {
        }
        public static boolean start(RootDoc root) {
            JavaDocReader.root = root;
            return true;
        }
    }
    public static ClassDoc[] extractJavaDoc() {
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URL[] urls = urlClassLoader.getURLs();
        StringBuilder classPathBuilder = new StringBuilder();
        for(URL url:urls){
            classPathBuilder.append(url.getPath().substring(1)+";");
        }

//        StringBuilder packageNameBuilder = new StringBuilder();
//        for(String packageName:QuickAPIConfig.packageNames){
//            packageNameBuilder.append(packageName);
//        }
        com.sun.tools.javadoc.Main.execute(new String[] {"-doclet",
                Doclet.class.getName(),
                "-encoding","utf-8",
                "-quiet",
                "-classpath",
                classPathBuilder.toString(),
                "-sourcepath",
                System.getProperty("user.dir")+"/src/main/java",
                QuickAPIConfig.packageNames.get(0)
        });
        return root.classes();
    }
}
