package cn.schoolwow.quickapi.util;

import cn.schoolwow.quickapi.domain.APIDocument;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Predicate;

public class QuickAPIConfig {
    /**接口文档路径*/
    public static String url = "/quickapi";
    /**指定生成路径*/
    public static String directory = "./src/main/resources";
    /**路径前缀*/
    public static String prefix = "";
    /**指定Java源代码路径*/
    public static StringBuilder sourcePathBuilder = new StringBuilder(System.getProperty("user.dir")+"/src/main/java;");
    /**指定类文件路径*/
    public static Set<URL> classPathList = new HashSet<>();
    /**控制器包名*/
    public static Set<String> controllerPackageNameList = new LinkedHashSet<>();
    /**控制器类*/
    public static List<String> controllerClassNameList = new ArrayList<>();
    /**要忽略的类*/
    public static List<String> ignoreClassList = new ArrayList<>();
    /**要忽略的包名*/
    public static List<String> ignorePackageNameList = new ArrayList<>(Arrays.asList(
            "java.",
            "javax.",
            "org.springframework",
            "cn.schoolwow.quickserver"
    ));
    /**函数式接口过滤类*/
    public static Predicate<String> predicate;
    /**api文件*/
    public static String apiJs;
    /**最终的APIDocument对象*/
    public static APIDocument apiDocument = new APIDocument();
    /**类加载器*/
    public static URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
}
