package cn.schoolwow.quickapi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**包扫描工具类*/
public class PackageUtil {
    private static Logger logger = LoggerFactory.getLogger(PackageUtil.class);

    /**是否是实体类包中的类 */
    public static boolean isInEntityPackage(String className){
        for(String packageName:QuickAPIConfig.entityPackageNameList){
            if(className.startsWith(packageName)){
                return true;
            }
        }
        return false;
    }

    /**扫描用户指定包中的类*/
    public static List<Class> scanPackage(String... packageNames){
        List<Class> classList = new ArrayList<>();
        for(String packageName:packageNames){
            String packageNamePath = packageName.replace(".", "/");
            try {
                Enumeration<URL> urlEnumeration = Thread.currentThread().getContextClassLoader().getResources(packageNamePath);
                while(urlEnumeration.hasMoreElements()){
                    URL url = urlEnumeration.nextElement();
                    if(url==null){
                        continue;
                    }
                    switch (url.getProtocol()) {
                        case "file": {
                            File file = new File(url.getFile());
                            //TODO 对于有空格或者中文路径会无法识别
                            logger.info("[类文件路径]{}", file.getAbsolutePath());
                            if (!file.isDirectory()) {
                                throw new IllegalArgumentException("包名不是合法的文件夹!" + url.getFile());
                            }
                            String indexOfString = packageName.replace(".", "/");
                            Files.walkFileTree(file.toPath(),new SimpleFileVisitor<Path>(){
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                        throws IOException
                                {
                                    File f = file.toFile();
                                    if(f.getName().endsWith(".class")){
                                        String path = f.getAbsolutePath().replace("\\", "/");
                                        int startIndex = path.indexOf(indexOfString);
                                        String className = path.substring(startIndex, path.length() - 6).replace("/", ".");
                                        try {
                                            classList.add(Class.forName(className));
                                        } catch (ClassNotFoundException e) {
                                            logger.warn("[实体类不存在]{}",className);
                                        }
                                    }
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        }
                        break;
                        case "jar": {
                            JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                            if (null != jarURLConnection) {
                                JarFile jarFile = jarURLConnection.getJarFile();
                                if (null != jarFile) {
                                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                                    while (jarEntries.hasMoreElements()) {
                                        JarEntry jarEntry = jarEntries.nextElement();
                                        String jarEntryName = jarEntry.getName();
                                        if (jarEntryName.contains(packageNamePath) && jarEntryName.endsWith(".class")) {
                                            String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                                            classList.add(Class.forName(className));
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if (classList.size() == 0) {
            logger.warn("[扫描实体类信息为空]");
            return classList;
        }
        return filterClass((classList));
    }

    /**根据规则过滤类*/
    private static List<Class> filterClass(List<Class> classList){
        Stream<Class> stream = classList.stream().filter((clazz)->{
            //排除枚举类型
            if(clazz.isEnum()){
                return false;
            }
            boolean result = true;
            //根据类过滤
            if(!QuickAPIConfig.ignoreClassList.isEmpty()){
                //为保证忽略内部类,需要以下处理
                for(String ignoreClassName:QuickAPIConfig.ignoreClassList){
                    if(clazz.getName().startsWith(ignoreClassName)){
                        logger.warn("[忽略类名]类名:{}!",clazz.getName());
                        result = false;
                    }
                }
            }
            //根据包名过滤
            if(!QuickAPIConfig.ignorePackageNameList.isEmpty()){
                for(String ignorePackageName:QuickAPIConfig.ignorePackageNameList){
                    if(clazz.getName().contains(ignorePackageName)){
                        logger.warn("[忽略包名]包名:{}类名:{}",ignorePackageName,clazz.getName());
                        result = false;
                    }
                }
            }
            return result;
        });
        if(null!=QuickAPIConfig.predicate){
            stream = stream.filter(QuickAPIConfig.predicate);
        }
        return stream.collect(Collectors.toList());
    }
}
