package cn.schoolwow.quickapi.util;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.handler.Handler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
                    apiHistory.addList.add(api.methods[0]+"_"+api.url);
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
                            apiHistory.addList.add(newAPI.methods[0]+"_"+newAPI.url);
                            logger.info("[新增接口]{} {} {}",newAPI.getName(),newAPI.methods[0],newAPI.url);
                            continue;
                        }
                        //判断是否变更
                        for(API oldAPI:oldAPIList){
                            if(newAPI.equals(oldAPI)&&!newAPI.apiParameters.equals(oldAPI.apiParameters)){
                                apiHistory.modifyList.add(newAPI.methods[0]+"_"+newAPI.url);
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
                            Files.copy(inputStream,path);
                        }
                    }
                };break;
            }
        }
    }

    /**生成swagger.json文件*/
    public static void generateSwagger() throws IOException {
        JSONObject o = new JSONObject();
        o.put("swagger","2.0");
        o.put("info",JSON.parseObject("{\"title\":\""+ apiDocument.title+"\",\"version\":\"last\"}"));
        o.put("basePath","/");
        //添加tag
        {
            JSONArray tagArray = new JSONArray();
            for(APIController apiController: apiDocument.apiControllerList){
                tagArray.add(JSON.parseObject("{\"name\":\""+apiController.getName()+"\",\"description\":null}"));
            }
            o.put("tags",tagArray);
        }
        o.put("schemes",JSON.parseArray("[\"http\"]"));
        //添加path
        {
            JSONObject paths = new JSONObject();
            for(APIController apiController: apiDocument.apiControllerList){
                for(API api:apiController.apiList){
                    JSONObject p = new JSONObject();
                    p.put("tags",JSON.parseArray("[\""+apiController.getName()+"\"]"));
                    p.put("summary",api.getName());
                    p.put("description",api.getDescription());
                    //添加参数
                    {
                        JSONArray parameters = new JSONArray();
                        for(APIParameter apiParameter:api.apiParameters){
                            JSONObject q = new JSONObject();
                            q.put("name",apiParameter.getName());
                            q.put("in",apiParameter.position);
                            q.put("required",apiParameter.required);
                            if(null==apiParameter.getDescription()){
                                q.put("description","");
                            }else{
                                q.put("description",apiParameter.getDescription()+("".equals(apiParameter.defaultValue)?"":",默认为"+apiParameter.defaultValue));
                            }
                            switch(apiParameter.requestType){
                                case "text":{
                                    q.put("type","string");
                                }break;
                                case "textarea":{
                                    q.put("name","root");
                                    p.put("consumes",JSON.parseArray("[\"application/json\"]"));
                                    JSONObject schema = new JSONObject();
                                    schema.put("$schema","http://json-schema.org/draft-04/schema#");
                                    schema.put("type","object");
                                    APIEntity apiEntity = apiDocument.apiEntityMap.get(apiParameter.type);
                                    if(null!=apiEntity){
                                        JSONObject fieldProperty = new JSONObject();
                                        if(null!=apiEntity.apiFields){
                                            for(APIField apiField:apiEntity.apiFields){
                                                fieldProperty.put(apiField.name,JSON.parseObject("{\"type\":\"string\",\"description\":\""+apiField.getDescription()+"\"}"));
                                            }
                                        }
                                        schema.put("properties",fieldProperty);
                                    }
                                    q.put("schema",schema);
                                }break;
                                case "file":{
                                    q.put("in","formData");
                                    q.put("type","file");
                                    q.put("description","上传的文件");
                                    p.put("consumes",JSON.parseArray("[\"multipart/form-data\"]"));
                                }break;
                            }
                            parameters.add(q);
                        }
                        p.put("parameters",parameters);
                    }
                    p.put("responses",JSON.parseObject("{\"200\":{\"description\":\"successful operation\",\"schema\":{}}}"));
                    paths.put(api.url,JSON.parseObject("{\""+api.methods[0].toLowerCase()+"\":"+p.toJSONString()+"}"));
                }
            }
            o.put("paths",paths);
        }
        Path path = Paths.get(QuickAPIConfig.directory+QuickAPIConfig.url+"/swagger.json");
        Files.createDirectories(path.getParent());
        Files.write(path,o.toJSONString().getBytes());
        logger.info("[生成swagger]路径:{}",path);
    }

    /**生成angularjs的service文件*/
    public static void generateAngularJS() throws IOException {
        StringBuilder builder = new StringBuilder();
        for(APIController apiController:apiDocument.apiControllerList){
            String name = apiController.getName().replace("Controller","Service");
            name = name.toLowerCase().charAt(0)+name.substring(1);
            builder.append("app.service(\"$"+name+"\",function($http,$httpParamSerializer){\n");
            for(API api:apiController.apiList){
                builder.append("\t/**"+(api.getName().startsWith("/")?api.getName().substring(1):api.getName())+"*/\n");
                builder.append("\tthis."+api.method.getName()+" = function(");
                if(!api.apiParameters.isEmpty()){
                    for(APIParameter apiParameter:api.apiParameters){
                        builder.append(apiParameter.getName()+",");
                    }
                    builder.deleteCharAt(builder.length()-1);
                }
                builder.append("){\n");
                if("multipart/form-data".equals(api.contentType)){
                    builder.append("\t\tlet fd = new FormData();\n");
                    for(APIParameter apiParameter:api.apiParameters){
                        if(apiParameter.type.startsWith("[L")){
                            builder.append("\t\tfor(let i=0;i<"+apiParameter.getName()+".length;i++){\n");
                            builder.append("\t\t\tfd.append(\""+apiParameter.getName()+"\","+apiParameter.getName()+"[i]);\n");
                            builder.append("\t\t}\n");
                        }else{
                            builder.append("\t\tfd.append(\""+apiParameter.getName()+"\","+apiParameter.getName()+");\n");
                        }
                    }
                }
                builder.append("\t\treturn $http({\n");
                builder.append("\t\t\turl:\""+api.url+"\",\n");
                builder.append("\t\t\tmethod:\""+api.methods[0]+"\",\n");
                if("multipart/form-data".equals(api.contentType)){
                    builder.append("\t\t\tdata:fd,\n");
                }else if("application/json".equals(api.contentType)){
                    builder.append("\t\t\tdata:"+api.apiParameters.get(0).getName()+",\n");
                }else{
                    if("GET".equals(api.methods[0])||"DELETE".equals(api.methods[0])){
                        builder.append("\t\t\tparams:{\n");
                    }else{
                        builder.append("\t\t\tdata:{\n");
                    }
                    for(APIParameter apiParameter:api.apiParameters){
                        builder.append("\t\t\t\t\""+apiParameter.getName()+"\":"+apiParameter.getName()+",\n");
                    }
                    builder.append("\t\t\t},\n");
                }
                if("application/x-www-form-urlencoded".equals(api.contentType)){
                    builder.append("\t\t\ttransformRequest: function (data) {\n");
                    builder.append("\t\t\t\treturn $httpParamSerializer(data);\n");
                    builder.append("\t\t\t},\n");
                }
                builder.append("\t\t\theaders:{\n");
                if("multipart/form-data".equals(api.contentType)){
                    api.contentType = "undefined";
                }
                builder.append("\t\t\t\t\"Content-Type\":\""+api.contentType+"\"\n");
                builder.append("\t\t\t},\n");
                builder.append("\t\t});\n");


                builder.append("\t};\n");
            }
            builder.append("});\n");
        }
        Path path = Paths.get(QuickAPIConfig.directory+QuickAPIConfig.url+"/service.js");
        Files.createDirectories(path.getParent());
        Files.write(path,builder.toString().getBytes());
        logger.info("[生成angularjs]路径:{}",path);
    }
}
