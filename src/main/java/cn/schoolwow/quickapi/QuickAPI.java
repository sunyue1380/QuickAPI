package cn.schoolwow.quickapi;

import cn.schoolwow.quickapi.domain.APIController;
import cn.schoolwow.quickapi.domain.APIDocument;
import cn.schoolwow.quickapi.handler.ControllerHandler;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class QuickAPI{
    public static QuickAPI newInstance(){
        return new QuickAPI();
    }

    public QuickAPI title(String title){
        QuickAPIConfig.title = title;
        return this;
    }

    public QuickAPI controller(String packageName){
        QuickAPIConfig.controllerPackageNameList.add(packageName);
        return this;
    }

    public QuickAPI entity(String packageName){
        QuickAPIConfig.entityPackageNameList.add(packageName);
        return this;
    }

    public QuickAPI url(String url){
        QuickAPIConfig.url = url;
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
        try {
            //生成API接口信息
            {
                List<APIController> apiControllerList = ControllerHandler.getAPIList();
                APIDocument apiDocument = new APIDocument();
                apiDocument.title = QuickAPIConfig.title;
                apiDocument.date = new Date();
                apiDocument.apiControllerList = apiControllerList;
                String data = JSON.toJSONString(apiDocument, SerializerFeature.DisableCircularReferenceDetect);
                File file = new File("./src/main/webapp"+QuickAPIConfig.url+"/api.json");
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
                                File file = new File("./src/main/webapp"+QuickAPIConfig.url+name);
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
