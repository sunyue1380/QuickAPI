package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.API;
import cn.schoolwow.quickapi.domain.APIController;
import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIParameter;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SpringMVCHandler extends AbstractHandler{
    private static LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
    /**需要忽略的注解*/
    private static Class[] ignoreAnnotationClasses = new Class[]{SessionAttribute.class};
    /**映射注解*/
    private static Class[] mappingClasses = new Class[]{
            GetMapping.class, PostMapping.class, PutMapping.class,DeleteMapping.class,PatchMapping.class
    };


    @Override
    public boolean exist() {
        try {
            ClassLoader.getSystemClassLoader().loadClass("org.springframework.web.bind.annotation.RequestMapping");
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
        //class必须要RestController或者Controller或者Component注解
        if(null==clazz.getAnnotation(Component.class)
                &&null==clazz.getAnnotation(Controller.class)
                &&null==clazz.getAnnotation(RestController.class)
        ){
            return null;
        }

        List<API> apiList = new ArrayList<>();
        for(Method method: clazz.getDeclaredMethods()){
            //判断MethodMapping
            API api = handleMethodMappingClass(method);
            if(null==api){
                api = handleRequestMapping(method);
            }
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
        apiController.name = clazz.getSimpleName();
        apiController.apiList = apiList;
        //是否有类上有RequestMapping注解
        RequestMapping classRequestMapping = (RequestMapping) clazz.getDeclaredAnnotation(RequestMapping.class);
        if(classRequestMapping!=null){
            String baseUrl = classRequestMapping.value()[0];
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
        String[] parameterNames = u.getParameterNames(api.method);
        List<APIParameter> apiParameterList = new ArrayList<>();
        List<String> parameterEntityNameList = new ArrayList<>();
        for(int i=0;i<parameters.length;i++){
            Class parameterType = parameters[i].getType();
            if(needIgnoreClass(parameterType.getName())){
                continue;
            }
            for(Class clazz:ignoreAnnotationClasses){
                if(null!=parameters[i].getAnnotation(clazz)){
                    continue;
                }
            }

            //处理泛型
            Type type = parameters[i].getParameterizedType();
            if(type instanceof ParameterizedType){
                ParameterizedType pType = (ParameterizedType)type;
                Type genericType = pType.getActualTypeArguments()[0];
                parameterEntityNameList.addAll(getRecycleEntity(genericType.getTypeName()));
            }

            APIParameter apiParameter = new APIParameter();
            apiParameter.parameter = parameters[i];
            //RequestParam
            {
                RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
                if(requestParam!=null){
                    apiParameter.name = requestParam.value();
                    if(apiParameter.name.isEmpty()){
                        apiParameter.name = requestParam.name();
                    }
                    apiParameter.required = requestParam.required();
                    if(!requestParam.defaultValue().equals(ValueConstants.DEFAULT_NONE)){
                        apiParameter.defaultValue = requestParam.defaultValue();
                    }
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
                RequestPart requestPart = parameters[i].getAnnotation(RequestPart.class);
                if(requestPart!=null||parameterType.getName().equals(MultipartFile.class.getName())){
                    if(null!=requestPart){
                        apiParameter.name = requestPart.value().isEmpty()?requestPart.name():requestPart.value();
                        apiParameter.required = requestPart.required();
                    }else{
                        apiParameter.name = parameters[i].getName();
                        apiParameter.required = true;
                    }
                    apiParameter.requestType = "file";
                    api.contentType = "multipart/form-data;";
                }
            }
            //RequestBody
            {
                RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
                if(requestBody!=null){
                    apiParameter.name = "requestBody";
                    apiParameter.required = requestBody.required();
                    apiParameter.requestType = "textarea";
                    api.contentType = "application/json; charset=utf-8";
                    parameterEntityNameList.addAll(getRecycleEntity(parameterType.getName()));
                }
            }
            //PathVaribale
            {
                PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
                if(pathVariable!=null){
                    apiParameter.name = pathVariable.value();
                    if(apiParameter.name.isEmpty()){
                        apiParameter.name = pathVariable.name();
                    }
                    apiParameter.required = pathVariable.required();
                    apiParameter.position = "query";
                }
            }
            if(apiParameter.name==null||apiParameter.name.isEmpty()){
                apiParameter.name = parameterNames[i];
            }
            apiParameter.type = parameters[i].getType().getName();
            apiParameterList.add(apiParameter);
        }
        api.parameterEntityNameList = parameterEntityNameList.toArray(new String[0]);
        api.apiParameters = apiParameterList.toArray(new APIParameter[0]);
    }

    private API handleMethodMappingClass(Method method){
        //判断是否有Mapping注解
        for(Class mappingClass:mappingClasses) {
            Annotation annotation = method.getDeclaredAnnotation(mappingClass);
            if(annotation == null){
                continue;
            }
            //存在mapping注解
            String requestMethod = mappingClass.getSimpleName().substring(0,mappingClass.getSimpleName().lastIndexOf("Mapping")).toUpperCase();
            API api = new API();
            api.methods = new String[]{requestMethod};
            try {
                String[] values = (String[]) mappingClass.getDeclaredMethod("value").invoke(annotation);
                api.url = values[0];
                if (api.url.charAt(0) != '/') {
                    api.url = "/" + api.url;
                }
            }catch (Exception e){
                continue;
            }
            return api;
        }
        return null;
    }

    private API handleRequestMapping(Method method){
        //判断RequestMapping
        RequestMapping methodRequestMapping = method.getDeclaredAnnotation(RequestMapping.class);
        if(null==methodRequestMapping){
            return null;
        }
        API api = new API();
        RequestMethod[] requestMethods = methodRequestMapping.method();
        if(requestMethods.length>0){
            api.methods = new String[requestMethods.length];
            for(int i=0;i<requestMethods.length;i++){
                api.methods[i] = requestMethods[i].name().toUpperCase();
            }
        }else{
            api.methods = new String[]{"all"};
        }
        api.url = methodRequestMapping.value()[0];
        if(api.url.charAt(0)!='/'){
            api.url = "/" + api.url;
        }
        return api;
    }
}
