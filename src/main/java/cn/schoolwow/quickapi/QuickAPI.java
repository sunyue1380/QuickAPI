package cn.schoolwow.quickapi;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.handler.controller.AbstractControllerHandler;
import cn.schoolwow.quickapi.handler.entity.AbstractEntityHandler;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import cn.schoolwow.quickdao.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sun.tools.javac.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class QuickAPI{
    private static Logger logger = LoggerFactory.getLogger(QuickAPI.class);
    public static QuickAPI newInstance(){
        return new QuickAPI();
    }

    private volatile APIDocument apiDocument;

    private QuickAPI(){
        if(null==apiDocument){
            synchronized (QuickAPI.class){
                if(null==apiDocument){
                    apiDocument = new APIDocument();
                }
            }
        }
    }

    /*文档标题*/
    public QuickAPI title(String title){
        apiDocument.title = title;
        return this;
    }

    /*文档描述*/
    public QuickAPI description(String description){
        apiDocument.description = description;
        return this;
    }

    /**controller层*/
    public QuickAPI controller(String packageName){
        QuickAPIConfig.controllerPackageNameList.add(packageName);
        return this;
    }

    /**Controller涉及的实体类层*/
    public QuickAPI entity(String packageName){
        QuickAPIConfig.entityPackageNameList.add(packageName);
        return this;
    }

    /**接口路径前缀*/
    public QuickAPI prefix(String prefix){
        apiDocument.prefix = prefix;
        return this;
    }

    /**文档路径地址*/
    public QuickAPI url(String url){
        QuickAPIConfig.url = url;
        return this;
    }

    /**文档生成目录*/
    public QuickAPI directory(String directory){
        QuickAPIConfig.directory = directory;
        return this;
    }

    /**Java源代码路径*/
    public QuickAPI sourcePath(String sourcePath){
        QuickAPIConfig.sourcePath = sourcePath;
        return this;
    }

    public QuickAPI ignorePackageName(String ignorePackageName){
        if(QuickAPIConfig.ignorePackageNameList==null){
            QuickAPIConfig.ignorePackageNameList = new ArrayList<>();
        }
        QuickAPIConfig.ignorePackageNameList.add(ignorePackageName);
        return this;
    }

    public QuickAPI ignoreClass(Class _class){
        if(QuickAPIConfig.ignoreClassList==null){
            QuickAPIConfig.ignoreClassList = new ArrayList<>();
        }
        QuickAPIConfig.ignoreClassList.add(_class);
        return this;
    }

    public QuickAPI filter(Predicate<Class> predicate){
        QuickAPIConfig.predicate = predicate;
        return this;
    }

    public QuickAPI generate(){
        //检测Java
        {
            File file = new File(QuickAPIConfig.sourcePath);
            if(!file.exists()){
                logger.warn("[源路径不存在]JavaDoc无法提取!请配置正确的源路径地址!当前源路径:{}",QuickAPIConfig.sourcePath);
            }
        }
        try {
            //生成API接口信息
            {
                apiDocument.date = new Date();
                apiDocument.apiControllerList = AbstractControllerHandler.apiControllerList;;
                apiDocument.apiEntityMap = AbstractEntityHandler.apiEntityMap;
                //生成json数据
                {
                    File file = new File(QuickAPIConfig.directory+QuickAPIConfig.url+"/api.json");
                    compareJSON(file,apiDocument);
                    String data = JSON.toJSONString(apiDocument, SerializerFeature.DisableCircularReferenceDetect);
                    generateFile(data,file);
                    QuickAPIConfig.jsonObject = data;
                }
                //生成swagger.json文件
                {
                    String data = generateSwagger(apiDocument);
                    File file = new File(QuickAPIConfig.directory+QuickAPIConfig.url+"/swagger.json");
                    generateFile(data,file);
                }
            }
            //复制静态资源文件
            {
                URL url = ClassLoader.getSystemResource("quickapi");
                switch(url.getProtocol()){
                    case "file":{

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

    /**上传到服务器*/
    public void upload() {
        upload("https://api.schoolwow.cn");
    }

    /**上传到服务器*/
    public void upload(String host) {
        if(QuickAPIConfig.jsonObject==null||QuickAPIConfig.jsonObject.isEmpty()){
            throw new IllegalArgumentException("请先调用generate()方法!");
        }
        StringBuilder sb = new StringBuilder();
        Scanner scanner = null;
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(host+"/api/project/uploadAPI").openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.getOutputStream().write(QuickAPIConfig.jsonObject.getBytes());
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

    /**比较新老JSON文件,获取变更信息*/
    private void compareJSON(File file,APIDocument newAPIDocument) throws FileNotFoundException {
        if(!file.exists()){
            return;
        }
        Scanner scanner = new Scanner(file);
        StringBuilder builder = new StringBuilder();
        while(scanner.hasNextLine()){
            builder.append(scanner.nextLine());
        }
        scanner.close();
        APIDocument oldAPIDocument = JSON.parseObject(builder.toString()).toJavaObject(APIDocument.class);
        //比对API
        List<APIController> oldAPIControllerList = oldAPIDocument.apiControllerList;
        List<APIController> newAPIControllerList = newAPIDocument.apiControllerList;
        //提取变更列表
        APIHistory apiHistory = new APIHistory();
        for(APIController newAPIController:newAPIControllerList){
            if(!oldAPIControllerList.contains(newAPIController)){
                List<API> newAPIList = newAPIController.apiList;
                for(API api:newAPIList){
                    apiHistory.addList.add(newAPIController.className+"#"+api.methods[0]+"_"+api.url);
                    logger.info("[新增接口]{} {} {}",api.name,api.methods[0],api.url);
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
                            apiHistory.addList.add(newAPIController.className+"#"+newAPI.methods[0]+"_"+newAPI.url);
                            logger.info("[新增接口]{} {} {}",newAPI.name,newAPI.methods[0],newAPI.url);
                            continue;
                        }
                        //判断是否变更
                        for(API oldAPI:oldAPIList){
                            if(newAPI.equals(oldAPI)&&!Arrays.equals(newAPI.apiParameters,oldAPI.apiParameters)){
                                apiHistory.modifyList.add(newAPIController.className+"#"+newAPI.methods[0]+"_"+newAPI.url);
                                logger.info("[变更接口]{} {} {}",newAPI.name,newAPI.methods[0],newAPI.url);
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
                    logger.info("[删除接口]{} {} {}",api.name,api.methods[0],api.url);
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
                            logger.info("[删除接口]{} {} {}",oldAPI.name,oldAPI.methods[0],oldAPI.url);
                        }
                    }
                    break;
                }
            }
        }
        if(!apiHistory.addList.isEmpty()||!apiHistory.modifyList.isEmpty()||!apiHistory.deleteList.isEmpty()){
            oldAPIDocument.apiHistoryList.add(0,apiHistory);
        }
        newAPIDocument.apiHistoryList = oldAPIDocument.apiHistoryList;
    }

    private String generateSwagger(APIDocument apiDocument){
        JSONObject o = new JSONObject();
        o.put("swagger","2.0");
        o.put("info",JSON.parseObject("{\"title\":\""+apiDocument.title+"\",\"version\":\"last\"}"));
        o.put("basePath","/");
        //添加tag
        {
            JSONArray tagArray = new JSONArray();
            for(APIController apiController:apiDocument.apiControllerList){
                tagArray.add(JSON.parseObject("{\"name\":\""+apiController.name+"\",\"description\":null}"));
            }
            o.put("tags",tagArray);
        }
        o.put("schemes",JSON.parseArray("[\"http\"]"));
        //添加path
        {
            JSONObject paths = new JSONObject();
            for(APIController apiController:apiDocument.apiControllerList){
                for(API api:apiController.apiList){
                    JSONObject p = new JSONObject();
                    p.put("tags",JSON.parseArray("[\""+apiController.name+"\"]"));
                    p.put("summary",api.name);
                    p.put("description",api.description);
                    //添加参数
                    {
                        JSONArray parameters = new JSONArray();
                        for(APIParameter apiParameter:api.apiParameters){
                            JSONObject q = new JSONObject();
                            q.put("name",apiParameter.name);
                            q.put("in",apiParameter.position);
                            q.put("required",apiParameter.required);
                            if(null==apiParameter.description){
                                q.put("description","");
                            }else{
                                q.put("description",apiParameter.description+("".equals(apiParameter.defaultValue)?"":",默认为"+apiParameter.defaultValue));
                            }
                            switch(apiParameter.requestType){
                                case "text":{
                                    q.put("type","string");
                                }break;
                                case "textarea":{
                                    q.put("name","root");
                                    p.put("consumes",JSON.parseArray("[\"application/json\"]"));
                                    APIEntity apiEntity = apiDocument.apiEntityMap.get(apiParameter.type);
                                    JSONObject schema = new JSONObject();
                                    schema.put("$schema","http://json-schema.org/draft-04/schema#");
                                    schema.put("type","object");
                                    JSONObject fieldProperty = new JSONObject();
                                    for(APIField apiField:apiEntity.apiFields){
                                        fieldProperty.put(apiField.name,JSON.parseObject("{\"type\":\"string\",\"description\":\""+apiField.description+"\"}"));
                                    }
                                    schema.put("properties",fieldProperty);
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
