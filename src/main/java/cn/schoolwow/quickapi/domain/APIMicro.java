package cn.schoolwow.quickapi.domain;

import cn.schoolwow.quickapi.domain.APIMicroService;

import java.net.URL;
import java.util.function.Predicate;

/**定义设置API微服务的接口*/
public interface APIMicro {
    /**
     * 扫描controller层
     * @param packageName 扫描Controller包
     * */
    APIMicroService controller(String packageName);

    /**
     * 扫描controller层
     * @param className 扫描单个Controller类
     * */
    APIMicroService controllerClass(String className);

    /**
     * 接口路径前缀
     * @param prefix 接口路径前缀(context-path)
     * */
    APIMicroService prefix(String prefix);

    /**
     * Java源代码路径
     * @param sourcePath 指定java源代码所在目录
     * */
    APIMicroService sourcePath(String sourcePath);

    /**
     * Java类路径
     * @param classPathURL 指定java类文件
     * */
    APIMicroService classPath(URL classPathURL);

    /**
     * 扫描pom.xml获取相关依赖
     * @param pomFilePath pom.xml路径
     * */
    APIMicroService pom(String pomFilePath);

    /**
     * 指定类库位置
     * @param libDirectory lib库位置
     * */
    APIMicroService lib(String libDirectory);

    /**
     * 忽略包名
     * @param ignorePackageName 要忽略的包名
     * */
    APIMicroService ignorePackageName(String ignorePackageName);

    /**
     * 忽略类
     * @param ignoreClassName 要忽略的类名
     * */
    APIMicroService ignoreClass(String ignoreClassName);

    /**
     * 扫描类过滤接口
     * @param predicate 函数式接口 参数为类名
     * */
    APIMicroService filter(Predicate<String> predicate);
}
