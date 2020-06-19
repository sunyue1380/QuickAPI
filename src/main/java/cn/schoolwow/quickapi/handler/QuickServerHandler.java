package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import cn.schoolwow.quickbeans.annotation.Component;
import cn.schoolwow.quickserver.annotation.*;
import cn.schoolwow.quickserver.request.MultipartFile;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuickServerHandler extends AbstractHandler{

    @Override
    public boolean exist() {
        try {
            QuickAPIConfig.urlClassLoader.loadClass("cn.schoolwow.quickserver.annotation.RequestMapping");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean isControllerEnvironment() {
        return true;
    }

    @Override
    public APIController getApiController(Class clazz) {
        if(null==clazz.getAnnotation(Component.class)){
            return null;
        }
        List<API> apiList = new ArrayList<>();
        for(Method method: clazz.getDeclaredMethods()){
            //判断MethodMapping
            API api = handleRequestMapping(method);
            if(null!=api){
                api.method = method;
                apiList.add(api);
            }
        }
        if(apiList.isEmpty()){
            return null;
        }
        APIController apiController = new APIController();
        apiController.clazz = clazz;
        apiController.className = clazz.getName();
        apiController.setName(clazz.getSimpleName());
        apiController.apiList = apiList;
        //是否有类上有RequestMapping注解
        RequestMapping classRequestMapping = (RequestMapping) clazz.getDeclaredAnnotation(RequestMapping.class);
        if(classRequestMapping!=null){
            String baseUrl = classRequestMapping.value();
            if(baseUrl.charAt(0)!='/'){
                baseUrl = "/"+baseUrl;
            }
            for(API api:apiController.apiList){
                api.url = baseUrl + api.url;
            }
        }
        for(API api:apiController.apiList){
            handleAPIParameter(api);
            handleReturnValue(api);
        }
        return apiController;
    }

    @Override
    public void handleController(APIController apiController) {

    }

    @Override
    public void handleAPI(API api) {

    }

    @Override
    public void handleEntity(APIEntity apiEntity) {

    }

    private void handleAPIParameter(API api){
        Parameter[] parameters = api.method.getParameters();
        List<APIParameter> apiParameterList = new ArrayList<>();
        List<String> parameterEntityNameList = new ArrayList<>();
        for(Parameter parameter:parameters){
            Class parameterType = parameter.getType();
            if(!parameterType.getName().equals(MultipartFile.class.getName())&&parameterType.getName().startsWith("cn.schoolwow.quickserver")){
                continue;
            }
            //处理复杂对象
            if(null==parameter.getDeclaredAnnotation(RequestBody.class)){
                for(APIField apiField: QuickAPIConfig.apiDocument.apiEntityMap.get(parameterType.getName()).apiFields){
                    if(apiField.ignore){
                        continue;
                    }
                    APIParameter apiParameter = new APIParameter();
                    apiParameter.setName(apiField.name);
                    apiParameter.setDescription(apiField.getDescription());
                    apiParameter.required = false;
                    apiParameter.type = apiField.className;
                    if(apiParameter.type.equals(MultipartFile.class.getName())){
                        apiParameter.requestType = "file";
                        api.contentType = "multipart/form-data;";
                    }
                    apiParameterList.add(apiParameter);
                }
                continue;
            }else{
                parameterEntityNameList.addAll(getRecycleEntity(parameterType.getName()));
            }

            //处理泛型
            Type type = parameter.getParameterizedType();
            if(type instanceof ParameterizedType){
                ParameterizedType pType = (ParameterizedType)type;
                Type genericType = pType.getActualTypeArguments()[0];
                parameterEntityNameList.add(genericType.getTypeName());
            }
            APIParameter apiParameter = new APIParameter();
            apiParameter.parameter = parameter;
            //RequestParam
            {
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                if(requestParam!=null){
                    apiParameter.setName(requestParam.name());
                    apiParameter.required = requestParam.required();
                    apiParameter.defaultValue = requestParam.defaultValue();
                    //存在post,put或者patch方法为body,否则为query
                    {
                        boolean existBodyMethod = false;
                        for(String requestMethod:api.methods){
                            if("POST".equalsIgnoreCase(requestMethod)
                                    ||"PUT".equalsIgnoreCase(requestMethod)
                                    ||"PATCH".equalsIgnoreCase(requestMethod)
                            ){
                                existBodyMethod = true;
                                break;
                            }
                        }
                        if(existBodyMethod){
                            apiParameter.position = "body";
                        }else{
                            apiParameter.position = "query";
                        }
                    }
                }
            }
            //RequestPart
            {
                RequestPart requestPart = parameter.getAnnotation(RequestPart.class);
                if(requestPart!=null||parameterType.getName().equals(MultipartFile.class.getName())){
                    apiParameter.setName(requestPart.name());
                    apiParameter.required = requestPart.required();
                    apiParameter.requestType = "file";
                    api.contentType = "multipart/form-data;";
                }
            }
            //RequestBody
            {
                RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
                if(requestBody!=null){
                    apiParameter.setName("requestBody");
                    apiParameter.required = requestBody.required();
                    apiParameter.requestType = "textarea";
                    api.contentType = "application/json; charset=utf-8";
                }
            }
            //PathVaribale
            {
                PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                if(pathVariable!=null){
                    apiParameter.setName(pathVariable.name());
                    apiParameter.required = pathVariable.required();
                    apiParameter.position = "path";
                }
            }
            if(parameterType.getName().equals(org.springframework.web.multipart.MultipartFile.class.getName())){
                apiParameter.setName(parameter.getName());
                apiParameter.requestType = "file";
                api.contentType = "multipart/form-data;";
            }
            if(null==apiParameter.getName()||apiParameter.getName().isEmpty()){
                continue;
            }
            apiParameter.type = parameter.getType().getName();
            apiParameterList.add(apiParameter);
        }
        api.apiParameters = apiParameterList.toArray(new APIParameter[0]);
    }

    private API handleRequestMapping(Method method){
        RequestMapping methodRequestMapping = method.getDeclaredAnnotation(RequestMapping.class);
        if(methodRequestMapping==null){
            return null;
        }
        RequestMethod[] requestMethods = methodRequestMapping.method();
        API api = new API();
        if(requestMethods.length>0){
            api.methods = new String[requestMethods.length];
            for(int i=0;i<requestMethods.length;i++){
                api.methods[i] = requestMethods[i].name().toUpperCase();
            }
        }else{
            api.methods = new String[]{"all"};
        }
        api.url = methodRequestMapping.value();
        return api;
    }
}
