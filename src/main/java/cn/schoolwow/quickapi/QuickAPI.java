package cn.schoolwow.quickapi;

import cn.schoolwow.quickapi.domain.APIMicro;
import cn.schoolwow.quickapi.domain.APIMicroService;
import cn.schoolwow.quickapi.domain.QuickAPIPlugin;
import cn.schoolwow.quickapi.util.GeneratorUtil;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Predicate;

import static cn.schoolwow.quickapi.util.QuickAPIConfig.apiDocument;

public class QuickAPI {
    private static Logger logger = LoggerFactory.getLogger(QuickAPI.class);

    public static QuickAPI newInstance(){
        return new QuickAPI();
    }

    private QuickAPI(){
    }

    /**
     * 扫描controller层
     * @param packageName 扫描Controller包
     * */
    public QuickAPI controller(String packageName){
        QuickAPIConfig.apiMicroService.controller(packageName);
        return this;
    }

    /**
     * 扫描controller层
     * @param className 扫描单个Controller类
     * */
    public QuickAPI controllerClass(String className){
        QuickAPIConfig.apiMicroService.controllerClass(className);
        return this;
    }

    /**
     * 接口路径前缀
     * @param prefix 接口路径前缀(context-path)
     * */
    public QuickAPI prefix(String prefix){
        QuickAPIConfig.apiMicroService.prefix(prefix);
        return this;
    }

    /**
     * Java源代码路径
     * @param sourcePath 指定java源代码所在目录
     * */
    public QuickAPI sourcePath(String sourcePath){
        QuickAPIConfig.apiMicroService.sourcePath(sourcePath);
        return this;
    }

    /**
     * Java类路径
     * @param classPathURL 指定java类文件
     * */
    public QuickAPI classPath(URL classPathURL){
        QuickAPIConfig.apiMicroService.classPath(classPathURL);
        return this;
    }

    /**
     * 扫描pom.xml获取相关依赖
     * @param pomFilePath pom.xml路径
     * */
    public QuickAPI pom(String pomFilePath) {
        QuickAPIConfig.apiMicroService.pom(pomFilePath);
        return this;
    }

    /**
     * 指定类库位置
     * @param libDirectory lib库位置
     * */
    public QuickAPI lib(String libDirectory) throws IOException {
        QuickAPIConfig.apiMicroService.lib(libDirectory);
        return this;
    }

    /**
     * 忽略包名
     * @param ignorePackageName 要忽略的包名
     * */
    public QuickAPI ignorePackageName(String ignorePackageName){
        QuickAPIConfig.apiMicroService.ignorePackageName(ignorePackageName);
        return this;
    }

    /**
     * 忽略类
     * @param ignoreClassName 要忽略的类名
     * */
    public QuickAPI ignoreClass(String ignoreClassName){
        QuickAPIConfig.apiMicroService.ignoreClass(ignoreClassName);
        return this;
    }

    /**
     * 扫描类过滤接口
     * @param predicate 函数式接口 参数为类名
     * */
    public QuickAPI filter(Predicate<String> predicate){
        QuickAPIConfig.apiMicroService.filter(predicate);
        return this;
    }
    
    /**
     * 指定文档标题
     * @param title 指定接口文档标题(唯一标识)
     * */
    public QuickAPI title(String title){
        apiDocument.title = title;
        return this;
    }

    /**
    * 设置文档描述
    * @param description 文档描述
    */
    public QuickAPI description(String description){
        apiDocument.description = description;
        return this;
    }

    /**
     * 文档路径地址
     * @param url 指定文档访问路径
     * */
    public QuickAPI url(String url){
        QuickAPIConfig.url = url;
        return this;
    }

    /**
     * 文档生成目录
     * @param directory 指定文档生成目录
     * */
    public QuickAPI directory(String directory){
        QuickAPIConfig.directory = directory;
        return this;
    }

    /**
     * 添加一个微服务
     * */
    public APIMicro apiMicro(){
        APIMicroService apiMicro = new APIMicroService();
        QuickAPIConfig.apiMicroServiceList.add(apiMicro);
        return apiMicro;
    }

    /**
     * 添加一个微服务
     * @param apiMicroServices 微服务列表
     * */
    public QuickAPI apiMicro(APIMicroService... apiMicroServices){
        QuickAPIConfig.apiMicroServiceList.addAll(Arrays.asList(apiMicroServices));
        return this;
    }

    /**生成接口文档*/
    public QuickAPI generate() throws IOException {
        if(null==apiDocument.apiControllerList){
            GeneratorUtil.handleApiDocument();
        }
        GeneratorUtil.generateApi();
        return this;
    }

    /**生成接口文档*/
    public QuickAPI plugin(QuickAPIPlugin... quickAPIPlugins) throws IOException {
        if(null==apiDocument.apiControllerList){
            GeneratorUtil.handleApiDocument();
        }
        for(QuickAPIPlugin quickAPIPlugin:quickAPIPlugins){
            quickAPIPlugin.plugin.generate();
        }
        return this;
    }

    /**上传到默认服务器*/
    public void upload() {
        upload("https://api.schoolwow.cn");
    }

    /**
     * 上传到服务器
     * @param host 服务器地址
     * */
    public void upload(String host) {
        upload(host,null);
    }
    /**
     * 上传到服务器
     * @param host 服务器地址
     * @param proxy http代理
     * */
    public void upload(String host, Proxy proxy) {
        if(null==apiDocument.apiControllerList){
            GeneratorUtil.handleApiDocument();
        }
        StringBuilder sb = new StringBuilder();
        Scanner scanner = null;
        HttpURLConnection httpURLConnection = null;
        try {
            if(null==proxy){
                httpURLConnection = (HttpURLConnection) new URL(host+"/api/projectVersion/uploadAPI").openConnection();
            }else{
                httpURLConnection = (HttpURLConnection) new URL(host+"/api/projectVersion/uploadAPI").openConnection(proxy);
            }
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type","application/json; charset=utf-8");
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            byte[] bytes = QuickAPIConfig.apiJs.getBytes();
            httpURLConnection.setFixedLengthStreamingMode(bytes.length);
            httpURLConnection.getOutputStream().write(bytes);
            httpURLConnection.getOutputStream().flush();
            httpURLConnection.connect();
            int statusCode = httpURLConnection.getResponseCode();

            if(statusCode==200){
                scanner = new Scanner(httpURLConnection.getInputStream());
            }else{
                scanner = new Scanner(httpURLConnection.getErrorStream());
            }
            while(scanner.hasNextLine()){
                sb.append(scanner.nextLine());
            }
            scanner.close();
            if(statusCode==200){
                logger.info("[上传成功]{}",sb.toString());
            }else{
                logger.warn("[上传失败]{}",sb.toString());
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if(null!=scanner){
                scanner.close();
            }
        }
    }
}
