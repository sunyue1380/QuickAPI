package cn.schoolwow.quickapi.util;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class JavaDocReader {
    private static ClassDoc[] classDocs = new ClassDoc[0];
    public static class Doclet {
        public Doclet() {
        }
        public static boolean start(RootDoc root) {
            classDocs = root.classes();
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

    public static ClassDoc[] getJavaDoc(Set<String> classNameSet){
        if(null==classNameSet||classNameSet.isEmpty()){
            return new ClassDoc[0];
        }
        Set<String> packageNameSet = new HashSet<>();
        for(String className:classNameSet){
            try {
                packageNameSet.add(ClassLoader.getSystemClassLoader().loadClass(className).getPackage().getName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        List<String> commands = new ArrayList<>(Arrays.asList("-doclet",
                Doclet.class.getName(),
                "-encoding","utf-8",
                "-private",
                "-quiet",
                "-classpath",
                classPath,
                "-sourcepath",
                QuickAPIConfig.sourcePath));
        Iterator<String> iterator = packageNameSet.iterator();
        while(iterator.hasNext()){
            commands.add("-subpackages");
            commands.add(iterator.next());
        }
        com.sun.tools.javadoc.Main.execute(commands.toArray(new String[0]));
        return classDocs;
    }
}
