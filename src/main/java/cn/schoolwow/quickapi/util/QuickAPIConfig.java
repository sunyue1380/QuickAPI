package cn.schoolwow.quickapi.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class QuickAPIConfig {
    /**接口文档路径*/
    public static String url = "/quickapi";
    /**指定生成路径*/
    public static String directory = "./src/main/resources";
    /**json对象*/
    public static String jsonObject;
    /**指定Java源代码路径*/
    public static String sourcePath = System.getProperty("user.dir")+"/src/main/java";
    /**控制器包名*/
    public static Set<String> controllerPackageNameList = new LinkedHashSet<>();
    /**控制器类*/
    public static List<Class> controllerClassList = new ArrayList<>();
    /**实体类包名*/
    public static Set<String> entityPackageNameList = new LinkedHashSet<>();
    /**实体类*/
    public static List<Class> entityClassList = new ArrayList<>();
    /**要忽略的类*/
    public static List<String> ignoreClassList = new ArrayList<>();
    /**要忽略的包名*/
    public static List<String> ignorePackageNameList = new ArrayList<>();
    /**函数式接口过滤类*/
    public static Predicate<Class> predicate;
}
