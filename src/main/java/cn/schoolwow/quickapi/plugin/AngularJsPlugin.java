package cn.schoolwow.quickapi.plugin;

import cn.schoolwow.quickapi.domain.API;
import cn.schoolwow.quickapi.domain.APIController;
import cn.schoolwow.quickapi.domain.APIParameter;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static cn.schoolwow.quickapi.util.QuickAPIConfig.apiDocument;

public class AngularJsPlugin implements Plugin{
    private Logger logger = LoggerFactory.getLogger(AngularJsPlugin.class);

    @Override
    public void generate() throws IOException {
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
