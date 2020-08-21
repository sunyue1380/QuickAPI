package cn.schoolwow.quickapi.util;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.handler.Handler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static cn.schoolwow.quickapi.util.QuickAPIConfig.apiDocument;
import static cn.schoolwow.quickapi.util.QuickAPIConfig.urlClassLoader;
import static cn.schoolwow.quickapi.util.QuickAPIUtil.getRecycleEntity;

public class GeneratorUtil {
    private static Logger logger = LoggerFactory.getLogger(GeneratorUtil.class);
    /**处理器*/
    private static Handler[] handlers;
    static{
        //动态加载Handler类
        Set<String> classNameSet = QuickAPIUtil.scanPackage("cn.schoolwow.quickapi.handler");
        List<Handler> handlerList = new ArrayList<>();
        for(String className:classNameSet){
            if(className.equals("cn.schoolwow.quickapi.handler.AbstractHandler")||className.equals("cn.schoolwow.quickapi.handler.Handler")){
                continue;
            }
            try {
                handlerList.add((Handler) ClassLoader.getSystemClassLoader().loadClass(className).newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        handlers = handlerList.toArray(new Handler[0]);
    }

    /**处理生成ApiDocument*/
    public static void handleApiDocument(){
        QuickAPIUtil.initClassPath();
        //扫描包
        Set<String> classNameSet = QuickAPIUtil.scanControllerPackage();
        List<APIController> apiControllerList = new ArrayList<>(classNameSet.size());
        //获取所有的Controller
        for(Handler handler:handlers){
            if(handler.exist()&&handler.isControllerEnvironment()){
                for(String className:classNameSet){
                    try {
                        Class clazz = urlClassLoader.loadClass(className);
                        APIController apiController = handler.getApiController(clazz);
                        if(null!=apiController){
                            apiControllerList.add(apiController);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //处理所有非控制器环境
        for(Handler handler:handlers){
            if(!handler.exist()){
                continue;
            }
            for(APIController apiController:apiControllerList){
                handler.handleController(apiController);
                if(null!=apiController.clazz.getAnnotation(Deprecated.class)){
                    apiController.deprecated = true;
                }
                for(API api:apiController.apiList){
                    handler.handleAPI(api);
                    if(null!=api.method.getAnnotation(Deprecated.class)){
                        api.deprecated = true;
                    }
                }
            }
        }
        //统一处理参数
        for(APIController apiController:apiControllerList){
            for(API api:apiController.apiList){
                api.url = QuickAPIConfig.prefix + api.url;
                for(APIParameter apiParameter:api.apiParameters){
                    //获取实际类型
                    apiParameter.entityType = QuickAPIUtil.getEntityClassName(apiParameter.type);
                    if(apiParameter.requestType.equals("text")&&!QuickAPIUtil.needIgnoreClass(apiParameter.entityType)){
                        api.parameterEntityNameList.addAll(getRecycleEntity(apiParameter.entityType));
                    }
                }
            }
        }
        for(Handler handler:handlers){
            if(!handler.exist()){
                continue;
            }
            for(APIEntity apiEntity:apiDocument.apiEntityMap.values()){
                handler.handleEntity(apiEntity);
            }
        }
        apiDocument.apiControllerList = apiControllerList;
        QuickAPIUtil.updateJavaDoc(classNameSet);
        for(APIController apiController:apiControllerList){
            for(API api:apiController.apiList){
                Iterator<APIParameter> iterator = api.apiParameters.iterator();
                List<APIParameter> extraAPIParamterList = new ArrayList<>();
                while(iterator.hasNext()){
                    APIParameter apiParameter = iterator.next();
                    if(apiParameter.requestType.equals("text")&&!apiParameter.parameter.getType().isPrimitive()&&!QuickAPIUtil.needIgnoreClass(apiParameter.entityType)){
                        iterator.remove();
                        APIEntity apiEntity = QuickAPIConfig.apiDocument.apiEntityMap.get(apiParameter.entityType);
                        for(APIField apiField:apiEntity.apiFields){
                            apiParameter = new APIParameter();
                            apiParameter.setName(apiField.name);
                            apiParameter.type = apiField.className;
                            apiParameter.entityType = QuickAPIUtil.getEntityClassName(apiParameter.type);
                            apiParameter.required = apiField.required;
                            if(apiParameter.type.startsWith("[L")){
                                if(apiParameter.type.contains("<")){
                                    apiParameter.requestType = "textarea";
                                    apiParameter.setDescription(apiField.getDescription()+"(多个参数请使用英文逗号分隔)");
                                }
                                String actualType = apiParameter.type.substring(2,apiParameter.type.length()-1);
                                if(actualType.equals(MultipartFile.class.getName())
                                        ||actualType.equals(cn.schoolwow.quickserver.request.MultipartFile.class.getName())){
                                    apiParameter.requestType = "file";
                                    apiParameter.setDescription(apiField.getDescription());
                                    api.contentType = "multipart/form-data";
                                }
                            }else{
                                apiParameter.setDescription(apiField.getDescription());
                            }
                            extraAPIParamterList.add(apiParameter);
                        }
                    }
                }
                api.apiParameters.addAll(extraAPIParamterList);
            }
        }
        //比较新旧json文件
        Path path = Paths.get(QuickAPIConfig.directory+QuickAPIConfig.url+"/api.js");
        if(Files.notExists(path)){
            return;
        }
        String content = "";
        try {
            content = new String(Files.readAllBytes(path));
        }catch (IOException e){
            e.printStackTrace();
            return;
        }
        if(!content.contains("{")||!content.contains("}")){
            return;
        }
        APIDocument oldAPIDocument = JSON.parseObject(content.substring(content.indexOf("{"),content.lastIndexOf("}")+1)).toJavaObject(APIDocument.class);
        //比对API
        List<APIController> oldAPIControllerList = oldAPIDocument.apiControllerList;
        List<APIController> newAPIControllerList = apiDocument.apiControllerList;
        //提取变更列表
        APIHistory apiHistory = new APIHistory();
        for(APIController newAPIController:newAPIControllerList){
            if(!oldAPIControllerList.contains(newAPIController)){
                List<API> newAPIList = newAPIController.apiList;
                for(API api:newAPIList){
                    apiHistory.addList.add(api.methods[0]+"_"+api.url+"_"+api.getDescription());
                    logger.info("[新增接口]{} {} {}",api.getName(),api.methods[0],api.url);
                }
                continue;
            }
            for(APIController oldAPIController:oldAPIControllerList){
                if(newAPIController.className.equals(oldAPIController.className)){
                    List<API> newAPIList = newAPIController.apiList;
                    List<API> oldAPIList = oldAPIController.apiList;
                    for(API newAPI:newAPIList){
                        //判断是否新增
                        if(!oldAPIList.contains(newAPI)){
                            apiHistory.addList.add(newAPI.methods[0]+"_"+newAPI.url+"_"+newAPI.getDescription());
                            logger.info("[新增接口]{} {} {}",newAPI.getName(),newAPI.methods[0],newAPI.url);
                            continue;
                        }
                        //判断是否变更
                        for(API oldAPI:oldAPIList){
                            if(newAPI.equals(oldAPI)&&!newAPI.apiParameters.equals(oldAPI.apiParameters)){
                                apiHistory.modifyList.add(newAPI.methods[0]+"_"+newAPI.url+"_"+newAPI.getDescription());
                                logger.info("[变更接口]{} {} {}",newAPI.getName(),newAPI.methods[0],newAPI.url);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        //判断是否删除
        for(APIController oldAPIController:oldAPIControllerList){
            if(!newAPIControllerList.contains(oldAPIController)){
                List<API> apiList = oldAPIController.apiList;
                for(API api:apiList){
                    apiHistory.deleteList.add(api);
                    logger.info("[删除接口]{} {} {}",api.getName(),api.methods[0],api.url);
                }
                continue;
            }
            for(APIController newAPIController:newAPIControllerList){
                if(oldAPIController.className.equals(newAPIController.className)){
                    List<API> oldAPIList = oldAPIController.apiList;
                    List<API> newAPIList = newAPIController.apiList;
                    for(API oldAPI:oldAPIList){
                        if(!newAPIList.contains(oldAPI)){
                            apiHistory.deleteList.add(oldAPI);
                            logger.info("[删除接口]{} {} {}",oldAPI.getName(),oldAPI.methods[0],oldAPI.url);
                        }
                    }
                    break;
                }
            }
        }
        if(!apiHistory.addList.isEmpty()||!apiHistory.modifyList.isEmpty()||!apiHistory.deleteList.isEmpty()){
            oldAPIDocument.apiHistoryList.add(0,apiHistory);
        }
        apiDocument.apiHistoryList = oldAPIDocument.apiHistoryList;
    }

    /**生成api.js文件*/
    public static void generateApi() throws IOException {
        //生成Api.js文件
        {
            Path path = Paths.get(QuickAPIConfig.directory+QuickAPIConfig.url+"/api.js");
            String data = "let apiDocument = "+ JSON.toJSONString(apiDocument, SerializerFeature.DisableCircularReferenceDetect)+";";
            Files.createDirectories(path.getParent());
            Files.write(path,data.getBytes());
            QuickAPIConfig.apiJs = data;
            logger.info("[文档路径]{}",path);
        }
        //复制静态资源文件
        {
            URL url = QuickAPIConfig.urlClassLoader.getResource("quickapi");
            switch(url.getProtocol()){
                case "file":{
                    Path target = Paths.get(QuickAPIConfig.directory+QuickAPIConfig.url);
                    if(!Files.exists(target)){
                        Files.createDirectory(target);
                    }
                    Path source = Paths.get(url.getPath().substring(1));
                    int sourceNameCount = source.getNameCount();
                    Files.walkFileTree(source,new SimpleFileVisitor<Path>(){
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                throws IOException {
                            if(dir.compareTo(source)!=0){
                                // 获取相对原路径的路径名，然后组合到target
                                Path subPath = target.resolve(dir.subpath(sourceNameCount, dir.getNameCount()));
                                Files.createDirectories(subPath);
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                throws IOException
                        {
                            if(file.toFile().getName().endsWith("api.js")){
                                return FileVisitResult.CONTINUE;
                            }
                            Files.copy(file, target.resolve(file.subpath(sourceNameCount, file.getNameCount())),
                                    StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                };break;
                case "jar":{
                    JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                    JarFile jarFile = jarURLConnection.getJarFile();
                    Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
                    while(jarEntryEnumeration.hasMoreElements()){
                        JarEntry jarEntry = jarEntryEnumeration.nextElement();
                        if(
                                jarEntry.getName().endsWith(".html")||
                                        jarEntry.getName().endsWith(".css")||
                                        jarEntry.getName().endsWith(".js")||
                                        jarEntry.getName().endsWith(".woff2")
                        ){
                            if(jarEntry.getName().endsWith("api.js")){
                                continue;
                            }
                            InputStream inputStream = jarFile.getInputStream(jarEntry);
                            String name = jarEntry.getName();
                            name = name.substring(name.indexOf("/"));
                            Path path = Paths.get(QuickAPIConfig.directory+QuickAPIConfig.url+name);
                            if(Files.notExists(path.getParent())){
                                Files.createDirectories(path.getParent());
                            }
                            Files.copy(inputStream,path,StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                };break;
            }
        }
    }
}
