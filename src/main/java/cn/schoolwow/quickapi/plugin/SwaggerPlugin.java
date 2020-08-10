package cn.schoolwow.quickapi.plugin;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static cn.schoolwow.quickapi.util.QuickAPIConfig.apiDocument;

public class SwaggerPlugin implements Plugin{
    private Logger logger = LoggerFactory.getLogger(SwaggerPlugin.class);

    @Override
    public void generate() throws IOException {
        JSONObject o = new JSONObject();
        o.put("swagger","2.0");
        o.put("info", JSON.parseObject("{\"title\":\""+ apiDocument.title+"\",\"version\":\"last\"}"));
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
}
