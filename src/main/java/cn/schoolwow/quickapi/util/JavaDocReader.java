package cn.schoolwow.quickapi.util;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

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
    public static String classPath;

    public static ClassDoc[] getJavaDoc(Set<String> classNameSet){
        if(null==classNameSet||classNameSet.isEmpty()){
            return new ClassDoc[0];
        }
        Set<String> packageNameSet = new HashSet<>();
        for(String className:classNameSet){
            try {
                String packageName = QuickAPIConfig.urlClassLoader.loadClass(className).getPackage().getName();
                //多个子目录只添加一次
                boolean find = false;
                for(String packageName0:packageNameSet){
                    if(packageName.startsWith(packageName0)){
                        find = true;
                        break;
                    }
                }
                if(!find){
                    packageNameSet.add(packageName);
                }
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
                QuickAPIConfig.sourcePathBuilder.toString()));
        Iterator<String> iterator = packageNameSet.iterator();
        while(iterator.hasNext()){
            commands.add("-subpackages");
            commands.add(iterator.next());
        }
        com.sun.tools.javadoc.Main.execute(commands.toArray(new String[0]));
        return classDocs;
    }
}
