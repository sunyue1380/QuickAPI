package cn.schoolwow.quickapi.handler.controller;

import cn.schoolwow.quickapi.domain.API;
import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIField;
import cn.schoolwow.quickapi.domain.APIParameter;
import cn.schoolwow.quickapi.util.PackageUtil;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class SpringMVCControllerHandler extends AbstractControllerHandler{
    private static LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
    private static Class[] mappingClasses = new Class[]{
            GetMapping.class,PostMapping.class,PutMapping.class,DeleteMapping.class,PatchMapping.class
    };

    @Override
    public String getBaseUrl(Class _class) {
        String baseUrl = "";
        RequestMapping classRequestMapping = (RequestMapping) _class.getDeclaredAnnotation(RequestMapping.class);
        if(classRequestMapping!=null){
            baseUrl = classRequestMapping.value()[0];
        }
        return baseUrl;
    }

    @Override
    public void handleRequestMapping(Method method, API api) {
        for(Class _class:mappingClasses){
            Annotation annotation = method.getDeclaredAnnotation(_class);
            if(annotation==null){
                continue;
            }
            String requestMethod = _class.getSimpleName().substring(0,_class.getSimpleName().lastIndexOf("Mapping")).toUpperCase();
            api.methods = new String[]{requestMethod};
            try {
                String[] values = (String[]) _class.getDeclaredMethod("value").invoke(annotation);
                api.url = values[0];
            }catch (Exception e){
                continue;
            }
            return;
        }
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
        api.url = methodRequestMapping.value()[0];
    }

    @Override
    public APIParameter[] handleParameter(Method method, API api) {
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = u.getParameterNames(method);
        List<APIParameter> apiParameterList = new ArrayList<>();
        List<String> parameterEntityNameList = new ArrayList<>();
        for(int i=0;i<parameters.length;i++){
            Class parameterType = parameters[i].getType();
            //排除特定类型的参数
            if(parameterType.getName().startsWith("javax.servlet")){
                continue;
            }
            //SessionAttribute
            {
                SessionAttribute sessionAttribute = parameters[i].getAnnotation(SessionAttribute.class);
                if(sessionAttribute!=null){
                    continue;
                }
            }
            //处理复杂对象
            if(PackageUtil.isInEntityPackage(parameterType.getName())){
                if(null==parameters[i].getDeclaredAnnotation(RequestBody.class)){
                    for(APIField apiField:apiEntityMap.get(parameterType.getName()).apiFields){
                        if(apiField.ignore){
                            continue;
                        }
                        APIParameter apiParameter = new APIParameter();
                        apiParameter.name = apiField.name;
                        apiParameter.description = apiField.description;
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
            }

            APIParameter apiParameter = new APIParameter();
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
                }
            }
            //RequestPart
            {
                RequestPart requestPart = parameters[i].getAnnotation(RequestPart.class);
                if(requestPart!=null||parameterType.getName().equals(MultipartFile.class.getName())){
                    apiParameter.name = requestPart.value();
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
                RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
                if(requestBody!=null){
                    apiParameter.name = "requestBody";
                    apiParameter.required = requestBody.required();
                    apiParameter.requestType = "textarea";
                    api.contentType = "application/json; charset=utf-8";
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
        return apiParameterList.toArray(new APIParameter[0]);
    }
}
