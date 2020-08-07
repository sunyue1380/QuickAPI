package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.API;
import cn.schoolwow.quickapi.domain.APIController;
import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIParameter;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
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

import static cn.schoolwow.quickapi.util.QuickAPIUtil.getRecycleEntity;

public class SpringMVCHandler extends AbstractHandler{

    @Override
    public boolean exist() {
        try {
            QuickAPIConfig.urlClassLoader.loadClass("org.springframework.web.bind.annotation.RequestMapping");
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
        apiController.setName(clazz.getSimpleName());
        apiController.apiList = apiList;
        //是否有类上有RequestMapping注解
        RequestMapping classRequestMapping = (RequestMapping) clazz.getDeclaredAnnotation(RequestMapping.class);
        if(classRequestMapping!=null){
            String baseUrl = "";
            if(classRequestMapping.value().length>0){
                baseUrl = classRequestMapping.value()[0];
            }else{
                baseUrl = clazz.getSimpleName().toLowerCase();
            }
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
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String[] parameterNames = u.getParameterNames(api.method);
        Parameter[] parameters = api.method.getParameters();
        List<APIParameter> apiParameterList = new ArrayList<>();
        List<String> parameterEntityNameList = new ArrayList<>();
        for(int i=0;i<parameters.length;i++){
            Class parameterType = parameters[i].getType();
            if(!parameterType.getName().equals(MultipartFile.class.getName())
                    &&(parameterType.getName().startsWith("javax.servlet")
                    ||parameterType.getName().startsWith("org.springframework"))
            ){
                continue;
            }
            if(null!=parameters[i].getAnnotation(SessionAttribute.class)){
                continue;
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
                    apiParameter.setName(requestParam.value());
                    apiParameter.setName(requestParam.name());
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
                if(requestPart!=null){
                    if(null!=requestPart){
                        apiParameter.setName(requestPart.value());
                        apiParameter.setName(requestPart.name());
                        apiParameter.required = requestPart.required();
                    }else{
                        apiParameter.setName(parameterNames[i]);
                        apiParameter.required = true;
                    }
                    apiParameter.requestType = "file";
                    api.contentType = "multipart/form-data";
                }
            }
            //RequestBody
            {
                RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
                if(requestBody!=null){
                    apiParameter.setName("requestBody");
                    apiParameter.required = requestBody.required();
                    apiParameter.requestType = "textarea";
                    api.contentType = "application/json";
                    parameterEntityNameList.addAll(getRecycleEntity(parameterType.getName()));
                }
            }
            //PathVaribale
            {
                PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
                if(pathVariable!=null){
                    apiParameter.setName(pathVariable.value());
                    apiParameter.setName(pathVariable.name());
                    apiParameter.required = pathVariable.required();
                    apiParameter.position = "query";
                }
            }
            if(parameterType.getName().equals(MultipartFile.class.getName())
                    ||parameterType.getName().startsWith("[L")&&parameterType.getName().substring(2,parameterType.getName().length()-1).equals(MultipartFile.class.getName())){
                apiParameter.requestType = "file";
                api.contentType = "multipart/form-data";
            }
            if(null==apiParameter.getName()||apiParameter.getName().isEmpty()){
                apiParameter.setName(parameterNames[i]);
            }
            apiParameter.type = parameterType.getName();
            apiParameterList.add(apiParameter);
        }
        api.parameterEntityNameList = parameterEntityNameList;
        api.apiParameters = apiParameterList;
    }

    private API handleMethodMappingClass(Method method){
        //判断是否有Mapping注解
        Class[] mappingClasses = new Class[]{
                GetMapping.class, PostMapping.class, PutMapping.class,DeleteMapping.class,PatchMapping.class};
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
        if(methodRequestMapping.value().length>0){
            api.url = methodRequestMapping.value()[0];
        }else{
            api.url = method.getName();
        }

        if(api.url.charAt(0)!='/'){
            api.url = "/" + api.url;
        }
        return api;
    }
}
