package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.util.JavaDocReader;
import cn.schoolwow.quickapi.util.PackageUtil;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class ControllerHandler {
    private static Logger logger = LoggerFactory.getLogger(ControllerHandler.class);
    private static LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();

    /**扫描控制类提取接口信息*/
    public static List<APIController> getAPIList() throws Exception {
        List<Class> classList = PackageUtil.scanPackage(QuickAPIConfig.controllerPackageNameList.toArray(new String[0]));
        List<APIController> apiControllerList = new ArrayList<>();
        Map<String,APIEntity> apiEntityMap = EntityHandler.getEntityList();
        for(Class _class:classList){
            String baseUrl = "";
            RequestMapping classRequestMapping = (RequestMapping) _class.getDeclaredAnnotation(RequestMapping.class);
            if(classRequestMapping!=null){
                baseUrl = classRequestMapping.value()[0];
            }
            APIController apiController = new APIController();
            apiController.className = _class.getName();
            apiController.tag = _class.getSimpleName();

            List<API> apiList = new ArrayList<>();
            for(Method method:_class.getDeclaredMethods()){
                RequestMapping methodRequestMapping = method.getDeclaredAnnotation(RequestMapping.class);
                if(methodRequestMapping==null){
                    continue;
                }
                API api = new API();
                api.methodName = method.getName();
                api.brief = api.methodName;
                //处理请求方法
                RequestMethod[] requestMethods = methodRequestMapping.method();
                if(requestMethods.length>0){
                    api.methods = new String[requestMethods.length];
                    for(int i=0;i<requestMethods.length;i++){
                        api.methods[i] = requestMethods[i].name().toUpperCase();
                    }
                }else{
                    api.methods = new String[]{"all"};
                }
                //处理请求路径
                api.url = baseUrl+methodRequestMapping.value()[0];
                //处理请求参数
                api.apiParameters = handleParameter(api,method);
                handleReturnValue(api,method,apiEntityMap);
                apiList.add(api);
            }
            if(apiList.size()==0){
                continue;
            }
            apiController.apiList = apiList;
            apiControllerList.add(apiController);
        }
        //处理注释
        {
            ClassDoc[] classDocs = JavaDocReader.getControllerJavaDoc();
            for(APIController apiController:apiControllerList){
                for(ClassDoc classDoc:classDocs){
                    if(apiController.className.equals(classDoc.qualifiedName())){
                        //获取tag
                        {
                            Tag[] tags = classDoc.tags("tag");
                            if(tags!=null&&tags.length>0){
                                apiController.tag = tags[0].text();
                            }
                        }
                        //获取brief和参数信息
                        {
                            MethodDoc[] methodDocs = classDoc.methods();
                            for(API api:apiController.apiList){
                                for(MethodDoc methodDoc:methodDocs){
                                    if(api.methodName.equals(methodDoc.name())){
                                        Tag[] briefs = methodDoc.tags("brief");
                                        if(briefs!=null&&briefs.length>0){
                                            api.brief = briefs[0].text();
                                        }
                                        api.description = methodDoc.commentText();
                                        //获取参数信息
                                        ParamTag[] paramTags = methodDoc.paramTags();
                                        for(APIParameter apiParameter:api.apiParameters){
                                            for(ParamTag paramTag:paramTags){
                                                if(apiParameter.name.equals(paramTag.parameterName())){
                                                    apiParameter.name = paramTag.parameterName();
                                                    apiParameter.description = paramTag.parameterComment();
                                                    break;
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        return apiControllerList;
    }

    /**提取请求参数相关信息*/
    private static APIParameter[] handleParameter(API api,Method method){
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = u.getParameterNames(method);
        List<APIParameter> apiParameterList = new ArrayList<>();
        for(int i=0;i<parameters.length;i++){
            //排除特定类型的参数
            if(parameters[i].getType().getName().startsWith("javax.servlet")){
                continue;
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
                if(requestPart!=null){
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
                    apiParameter.requestType = "textarea";
                    api.contentType = "application/json; charset=utf-8";
                }
            }
            //PathVaribale
            {
                PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
                if(pathVariable!=null){
                    apiParameter.position = "query";
                }
            }
            if(apiParameter.name==null||apiParameter.name.isEmpty()){
                apiParameter.name = parameterNames[i];
                apiParameter.required = true;
            }
            apiParameter.type = parameters[i].getType().getName();
            apiParameterList.add(apiParameter);
        }
        return apiParameterList.toArray(new APIParameter[0]);
    }

    /**提取请求参数相关信息*/
    private static void handleReturnValue(API api,Method method,Map<String,APIEntity> apiEntityMap){
        api.returnValue = method.getGenericReturnType().getTypeName();
        Set<APIEntity> apiEntitySet = new LinkedHashSet<>();
        handleReturnEntity(method.getReturnType().getName(),apiEntitySet,apiEntityMap);
        //处理泛型
        {
            Type genericReturnType = method.getGenericReturnType();
            if(genericReturnType instanceof ParameterizedType){
                Type[] types = ((ParameterizedType)genericReturnType).getActualTypeArguments();
                for(Type type:types){
                    handleReturnEntity(type.getTypeName(),apiEntitySet,apiEntityMap);
                }
            }
        }
        api.returnEntityList = apiEntitySet.toArray(new APIEntity[0]);
    }

    /**处理返回类实体*/
    private static void handleReturnEntity(String className, Set<APIEntity> apiEntitySet, Map<String,APIEntity> apiEntityMap){
        if(!apiEntityMap.containsKey(className)){
            return;
        }
        APIEntity apiEntity = apiEntityMap.get(className);
        apiEntitySet.add(apiEntity);
        for(APIField apiField:apiEntity.apiFields){
            APIEntity fieldEntity = apiEntityMap.get(apiField.className);
            if(fieldEntity!=null){
                apiEntitySet.add(fieldEntity);
            }
        }
    }
}
