package cn.schoolwow.quickapi.util;

import cn.schoolwow.quickapi.domain.APIDocument;

import java.util.*;
import java.util.function.Predicate;

public class QuickAPIConfig {
    /**接口文档路径*/
    public static String url = "/quickapi";
    /**指定生成路径*/
    public static String directory = "./src/main/resources";
    /**api文件*/
    public static String apiJs;
    /**指定Java源代码路径*/
    public static String sourcePath = System.getProperty("user.dir")+"/src/main/java";
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
    /**最终的APIDocument对象*/
    public static APIDocument apiDocument = new APIDocument();
}
