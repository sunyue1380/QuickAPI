package cn.schoolwow.quickapi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class QuickAPIConfig {
    /**接口文档标题*/
    public static String title = "QuickAPI";
    /**接口文档路径*/
    public static String url = "/quickapi";
    /**指定生成路径*/
    public static String directory = "./src/main/webapp";
    /**指定Java源代码路径*/
    public static String sourcePath = System.getProperty("user.dir")+"/src/main/java";
    /**描述*/
    public static String description = "";
    /**控制器包名*/
    public static List<String> controllerPackageNameList = new ArrayList<>();
    /**实体类包名*/
    public static List<String> entityPackageNameList = new ArrayList<>();
    /**要忽略的类*/
    public static List<Class> ignoreClassList;
    /**要忽略的包名*/
    public static List<String> ignorePackageNameList;
    /**函数式接口过滤类*/
    public static Predicate<Class> predicate;
}
