package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.API;
import cn.schoolwow.quickapi.domain.APIParameter;
import cn.schoolwow.quickserver.annotation.*;
import cn.schoolwow.quickserver.request.MultipartFile;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class QuickServerControllerHandler extends AbstractControllerHandler{
    @Override
    public String getBaseUrl(Class _class) {
        String baseUrl = "";
        RequestMapping classRequestMapping = (RequestMapping) _class.getDeclaredAnnotation(RequestMapping.class);
        if(classRequestMapping!=null){
            baseUrl = classRequestMapping.value();
        }
        return baseUrl;
    }

    @Override
    public void handleRequestMapping(Method method, API api) {
        RequestMapping methodRequestMapping = method.getDeclaredAnnotation(RequestMapping.class);
        if(methodRequestMapping==null){
            return;
        }
        RequestMethod[] requestMethods = methodRequestMapping.method();
        if(requestMethods.length>0){
            api.methods = new String[requestMethods.length];
            for(int i=0;i<requestMethods.length;i++){
                api.methods[i] = requestMethods[i].name().toUpperCase();
            }
        }else{
            api.methods = new String[]{"all"};
        }
        api.url = methodRequestMapping.value();
    }

    @Override
    public APIParameter[] handleParameter(Method method, API api) {
        Parameter[] parameters = method.getParameters();
        List<APIParameter> apiParameterList = new ArrayList<>();
        for(Parameter parameter:parameters){
            Class parameterType = parameter.getType();
            //排除特定类型的参数
            if(parameterType.getName().startsWith("cn.schoolwow.quickserver")){
                continue;
            }
            APIParameter apiParameter = new APIParameter();
            //RequestParam
            {
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                if(requestParam!=null){
                    apiParameter.name = requestParam.name();
                    if(apiParameter.name.isEmpty()){
                        apiParameter.name = requestParam.name();
                    }
                    apiParameter.required = requestParam.required();
                    apiParameter.defaultValue = requestParam.defaultValue();
                }
            }
            //RequestPart
            {
                RequestPart requestPart = parameter.getAnnotation(RequestPart.class);
                if(requestPart!=null||parameterType.getName().equals(MultipartFile.class.getName())){
                    apiParameter.name = requestPart.name();
                    if(apiParameter.name.isEmpty()){
                        apiParameter.name = requestPart.name();
                    }
                    apiParameter.required = requestPart.required();
                    apiParameter.requestType = "file";
                    api.contentType = "multipart/form-data;";
                }
            }
            //RequestBody
            {
                RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
                if(requestBody!=null){
                    apiParameter.requestType = "textarea";
                    api.contentType = "application/json; charset=utf-8";
                }
            }
            //PathVaribale
            {
                PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                if(pathVariable!=null){
                    apiParameter.name = pathVariable.name();
                    if(apiParameter.name.isEmpty()){
                        apiParameter.name = pathVariable.name();
                    }
                    apiParameter.required = pathVariable.required();
                    apiParameter.position = "query";
                }
            }
            if(apiParameter.name==null||apiParameter.name.isEmpty()){
                apiParameter.name = parameter.getName();
            }
            apiParameter.type = parameter.getType().getName();
            apiParameterList.add(apiParameter);
        }
        return apiParameterList.toArray(new APIParameter[0]);
    }
}
