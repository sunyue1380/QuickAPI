package cn.schoolwow.quickapi.util;

import cn.schoolwow.quickapi.domain.APIDocument;
import cn.schoolwow.quickapi.domain.APIMicroService;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuickAPIConfig{
    /**接口文档路径*/
    public static String url = "/quickapi";
    /**指定生成路径*/
    public static String directory = "./src/main/resources";
    /**默认微服务*/
    public static APIMicroService apiMicroService = new APIMicroService();
    /**微服务*/
    public static List<APIMicroService> apiMicroServiceList = new ArrayList<>();
    static{
        apiMicroServiceList.add(apiMicroService);
    }
    /**指定Java源代码路径*/
    public static StringBuilder sourcePathBuilder = new StringBuilder(System.getProperty("user.dir")+"/src/main/java;");
    /**指定类文件路径*/
    public static Set<URL> classPathList = new HashSet<>();
    /**api文件*/
    public static String apiJs;
    /**最终的APIDocument对象*/
    public static APIDocument apiDocument = new APIDocument();
    /**类加载器*/
    public static URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
}
