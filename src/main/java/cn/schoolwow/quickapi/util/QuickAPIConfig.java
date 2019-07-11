package cn.schoolwow.quickapi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class QuickAPIConfig {
    /**接口文档标题*/
    public static String title = "QuickAPI";
    /**要扫描的包名*/
    public static List<String> packageNames = new ArrayList<>();
    /**要忽略的类*/
    public static List<Class> ignoreClassList;
    /**要忽略的包名*/
    public static List<String> ignorePackageNameList;
    /**函数式接口过滤类*/
    public static Predicate<Class> predicate;
}
