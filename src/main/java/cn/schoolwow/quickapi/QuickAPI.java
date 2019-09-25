package cn.schoolwow.quickapi;

import cn.schoolwow.quickapi.domain.APIController;
import cn.schoolwow.quickapi.domain.APIDocument;
import cn.schoolwow.quickapi.handler.controller.AbstractControllerHandler;
import cn.schoolwow.quickapi.handler.controller.ControllerHandler;
import cn.schoolwow.quickapi.handler.controller.QuickServerControllerHandler;
import cn.schoolwow.quickapi.handler.controller.SpringMVCControllerHandler;
import cn.schoolwow.quickapi.handler.entity.AbstractEntityHandler;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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

    /*文档标题*/
    public QuickAPI title(String title){
        QuickAPIConfig.title = title;
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

    /**文档描述*/
    public QuickAPI description(String description){
        QuickAPIConfig.description = description;
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

    public void generate(){
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
                APIDocument apiDocument = new APIDocument();
                apiDocument.title = QuickAPIConfig.title;
                apiDocument.description = QuickAPIConfig.description;
                apiDocument.date = new Date();
                apiDocument.apiControllerList = AbstractControllerHandler.apiControllerList;;
                apiDocument.apiEntityMap = AbstractEntityHandler.apiEntityMap;
                String data = JSON.toJSONString(apiDocument, SerializerFeature.DisableCircularReferenceDetect);
                File file = new File(QuickAPIConfig.directory+QuickAPIConfig.url+"/api.json");
                logger.debug("[生成文件]路径:{}",file.getAbsolutePath());
                generateFile(data,file);
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
                                    jarEntry.getName().endsWith(".js")
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
