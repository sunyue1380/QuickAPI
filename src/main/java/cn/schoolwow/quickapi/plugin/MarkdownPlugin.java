package cn.schoolwow.quickapi.plugin;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import static cn.schoolwow.quickapi.util.QuickAPIConfig.apiDocument;

public class MarkdownPlugin implements Plugin{
    private Logger logger = LoggerFactory.getLogger(MarkdownPlugin.class);
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Override
    public void generate() throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("# "+apiDocument.title+"\n\n");
        if(null!=apiDocument.description&&!apiDocument.description.isEmpty()){
            builder.append(apiDocument.description+"\n\n");
        }
        builder.append("[TOC]\n\n");
        if(!apiDocument.apiHistoryList.isEmpty()){
            builder.append("# 历史记录\n\n");
            for(APIHistory apiHistory: apiDocument.apiHistoryList){
                builder.append("## "+sdf.format(apiHistory.updateTime)+"\n\n");
                builder.append("|操作|方法|地址|描述|\n");
                builder.append("|:---:|:---:|:---:|:---:|\n");
                for(String history: apiHistory.addList){
                    StringTokenizer stringTokenizer = new StringTokenizer(history,"_");
                    builder.append("|**新增**");
                    builder.append("|"+stringTokenizer.nextToken());
                    builder.append("|"+stringTokenizer.nextToken());
                    builder.append("|"+stringTokenizer.nextToken());
                    builder.append("|\n");
                }
                for(String history: apiHistory.modifyList){
                    StringTokenizer stringTokenizer = new StringTokenizer(history,"_");
                    builder.append("|**变更**");
                    builder.append("|"+stringTokenizer.nextToken());
                    builder.append("|"+stringTokenizer.nextToken());
                    builder.append("|"+stringTokenizer.nextToken());
                    builder.append("|\n");
                }
                for(API api: apiHistory.deleteList){
                    builder.append("|**删除**");
                    builder.append("|"+api.methods[0]);
                    builder.append("|"+api.url);
                    builder.append("|"+api.getDescription());
                    builder.append("|\n");
                }
                builder.append("\n");
            }
        }
        builder.append("\n");
        builder.append("# 接口列表\n\n");
        for(APIController apiController:apiDocument.apiControllerList){
            builder.append("## "+apiController.getName()+"\n\n");
            for(API api:apiController.apiList){
                if(api.deprecated){
                    builder.append("### ~~"+api.getName()+"~~\n\n");
                }else{
                    builder.append("### "+api.getName()+"\n\n");
                }
                if(null!=api.getDescription()&&!api.getDescription().isEmpty()){
                    builder.append("> "+api.getDescription()+"\n\n");
                }
                builder.append("**"+api.methods[0]+"** "+api.url+"\n\n");
                builder.append("**"+api.contentType+"**\n\n");
                builder.append("**请求参数**\n\n");
                if(api.apiParameters.isEmpty()){
                    builder.append("当前请求不需要传递参数\n");
                }else{
                    builder.append("|参数名称|类型|描述|是否必须|默认值|\n");
                    builder.append("|:---:|:---:|:---:|:---:|:---:|\n");
                    for(APIParameter apiParameter:api.apiParameters){
                        builder.append("|"+apiParameter.getName());
                        builder.append("|"+apiParameter.type);
                        builder.append("|"+apiParameter.getDescription());
                        builder.append("|"+apiParameter.required);
                        builder.append("|"+apiParameter.defaultValue);
                        builder.append("|\n");
                    }
                }
                builder.append("\n");
                if(!api.parameterEntityNameList.isEmpty()){
                    builder.append("**关联实体**\n\n");
                    for(String entityName:api.parameterEntityNameList){
                        APIEntity apiEntity = apiDocument.apiEntityMap.get(entityName);
                        builder.append(apiEntity.simpleName+"\n\n");
                        builder.append("|字段名|字段类型|描述|\n");
                        builder.append("|:---:|:---:|:---:|\n");
                        for(APIField apiField:apiEntity.apiFields){
                            builder.append("|"+apiField.name);
                            builder.append("|"+apiField.className);
                            builder.append("|"+apiField.getDescription());
                            builder.append("|\n");
                        }
                        builder.append("\n");
                    }
                }
                builder.append("\n **返回值**\n\n"+api.returnValue+"\n\n");
                if(api.returnEntityNameList.length>0){
                    builder.append("**关联实体**\n\n");
                    for(String entityName:api.returnEntityNameList){
                        APIEntity apiEntity = apiDocument.apiEntityMap.get(entityName);
                        builder.append(apiEntity.simpleName+"\n\n");
                        builder.append("|字段名|字段类型|描述|\n");
                        builder.append("|:---:|:---:|:---:|\n");
                        for(APIField apiField:apiEntity.apiFields){
                            builder.append("|"+apiField.name);
                            builder.append("|"+apiField.className);
                            builder.append("|"+apiField.getDescription());
                            builder.append("|\n");
                        }
                        builder.append("\n");
                    }
                }
                builder.append("\n\n");
            }
            builder.append("\n\n\n");
        }
        Path path = Paths.get(QuickAPIConfig.directory+QuickAPIConfig.url+"/"+apiDocument.title+".md");
        Files.createDirectories(path.getParent());
        Files.write(path,builder.toString().getBytes());
        logger.info("[生成Markdown文件]路径:{}",path);
    }
}
