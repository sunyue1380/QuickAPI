package cn.schoolwow.quickapi;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.handler.Handler;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import cn.schoolwow.quickapi.util.QuickAPIUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static cn.schoolwow.quickapi.util.QuickAPIConfig.apiDocument;
import static cn.schoolwow.quickapi.util.QuickAPIConfig.urlClassLoader;
import static cn.schoolwow.quickapi.util.QuickAPIUtil.getRecycleEntity;

public class QuickAPI{
    private static Logger logger = LoggerFactory.getLogger(QuickAPI.class);
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
    public static QuickAPI newInstance(){
        return new QuickAPI();
    }

    private QuickAPI(){
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
     * 扫描controller层
     * @param packageName 扫描Controller包
     * */
    public QuickAPI controller(String packageName){
        QuickAPIConfig.controllerPackageNameList.add(packageName);
        return this;
    }

    /**
     * 扫描controller层
     * @param className 扫描单个Controller类
     * */
    public QuickAPI controllerClass(String className){
        QuickAPIConfig.controllerClassNameList.add(className);
        return this;
    }

    /**
     * 接口路径前缀
     * @param prefix 接口路径前缀(context-path)
     * */
    public QuickAPI prefix(String prefix){
        if(null==prefix||prefix.isEmpty()){
            return this;
        }
        if(!prefix.startsWith("/")){
            prefix = "/"+prefix;
        }
        QuickAPIConfig.prefix = prefix;
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
     * Java源代码路径
     * @param sourcePath 指定java源代码所在目录
     * */
    public QuickAPI sourcePath(String sourcePath){
        //判断文件目录是否存在
        if(Files.notExists(Paths.get(sourcePath))){
            logger.warn("[源文件路径不存在]路径:{}",sourcePath);
            return this;
        }
        QuickAPIConfig.sourcePathBuilder.append(sourcePath+";");
        return this;
    }

    /**
     * Java类路径
     * @param classPathURL 指定java类文件
     * */
    public QuickAPI classPath(URL classPathURL){
        QuickAPIConfig.classPathList.add(classPathURL);
        return this;
    }

    /**
     * 指定类库位置
     * @param libDirectory lib库位置
     * */
    public QuickAPI lib(String libDirectory) throws IOException {
        Files.walkFileTree(Paths.get(libDirectory), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(file.toFile().getName().endsWith(".jar")){
                    classPath(file.toUri().toURL());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return this;
    }

    /**
     * 忽略包名
     * @param ignorePackageName 要忽略的包名
     * */
    public QuickAPI ignorePackageName(String ignorePackageName){
        QuickAPIConfig.ignorePackageNameList.add(ignorePackageName);
        return this;
    }

    /**
     * 忽略类
     * @param ignoreClassName 要忽略的类名
     * */
    public QuickAPI ignoreClass(String ignoreClassName){
        QuickAPIConfig.ignoreClassList.add(ignoreClassName);
        return this;
    }

    /**
     * 扫描类过滤接口
     * @param predicate 函数式接口 参数为类名
     * */
    public QuickAPI filter(Predicate<String> predicate){
        QuickAPIConfig.predicate = predicate;
        return this;
    }

    /**生成接口文档*/
    public QuickAPI generate(){
        try {
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
                for(APIEntity apiEntity:apiDocument.apiEntityMap.values()){
                    handler.handleEntity(apiEntity);
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
                                if(apiParameter.type.startsWith("[L")||(apiParameter.type.contains("<"))){
                                    apiParameter.requestType = "textarea";
                                    apiParameter.setDescription(apiField.getDescription()+"(多个参数请使用英文逗号分隔)");
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

            //生成API接口信息
            {
                //生成json数据
                {
                    File file = new File(QuickAPIConfig.directory+QuickAPIConfig.url+"/api.js");
                    compareJSON(file);
                    String data = "let apiDocument = "+JSON.toJSONString(apiDocument, SerializerFeature.DisableCircularReferenceDetect)+";";
                    generateFile(data,file);
                    QuickAPIConfig.apiJs = data;
                    logger.info("[文档路径]{}",file.getAbsolutePath());
                }
                //生成swagger.json文件
                {
                    String data = generateSwagger();
                    File file = new File(QuickAPIConfig.directory+QuickAPIConfig.url+"/swagger.json");
                    generateFile(data,file);
                }
            }
            //复制静态资源文件
            {
                URL url = QuickAPIConfig.urlClassLoader.getSystemResource("quickapi");
                switch(url.getProtocol()){
                    case "file":{
                        Path target = Paths.get(QuickAPIConfig.directory+QuickAPIConfig.url);
                        if(!Files.exists(target)){
                            Files.createDirectory(target);
                        }
                        Path source = Paths.get(url.toURI());
                        int sourceNameCount = source.getNameCount();
                        Files.walkFileTree(source,new SimpleFileVisitor<Path>(){
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                    throws IOException {
                                if(dir.compareTo(source)!=0){
                                    // 获取相对原路径的路径名，然后组合到target上
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
                                File file = new File(QuickAPIConfig.directory+QuickAPIConfig.url+name);
                                generateFile(inputStream,file);
                            }
                        }
                    };break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        if(QuickAPIConfig.apiJs ==null||QuickAPIConfig.apiJs.isEmpty()){
            throw new IllegalArgumentException("请先调用generate()方法!");
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

    /**比较新老JS文件,获取变更信息*/
    private void compareJSON(File file) throws FileNotFoundException {
        if(!file.exists()){
            return;
        }
        Scanner scanner = new Scanner(file);
        StringBuilder builder = new StringBuilder();
        while(scanner.hasNextLine()){
            builder.append(scanner.nextLine());
        }
        scanner.close();
        if(builder.length()==0||builder.indexOf("{")<0){
            return;
        }
        String json = builder.substring(builder.indexOf("{"),builder.lastIndexOf("}")+1);
        APIDocument oldAPIDocument = JSON.parseObject(json).toJavaObject(APIDocument.class);
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

    private String generateSwagger(){
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
        return o.toJSONString();
    }

    private void generateFile(InputStream inputStream,File file) throws IOException {
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
        byte[] bytes = new byte[8192];
        int length=-1;
        while((length=inputStream.read(bytes,0,bytes.length))!=-1){
            fos.write(bytes,0,length);
        }
        fos.flush();
        fos.close();
    }

    private void generateFile(String data,File file) throws FileNotFoundException {
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        PrintWriter pw = new PrintWriter(file);
        pw.print(data);
        pw.flush();
        pw.close();
    }
}
