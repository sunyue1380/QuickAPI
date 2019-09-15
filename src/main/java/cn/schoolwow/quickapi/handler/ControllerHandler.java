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
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class ControllerHandler {
    private static Logger logger = LoggerFactory.getLogger(ControllerHandler.class);
    private static LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
    private static Map<String,APIEntity> apiEntityMap = EntityHandler.getEntityList();
    private static Class[] mappingClasses = new Class[]{
            GetMapping.class,PostMapping.class,PutMapping.class,DeleteMapping.class,PatchMapping.class
    };

    /**扫描控制类提取接口信息*/
    public static List<APIController> getAPIList(){
        List<Class> classList = PackageUtil.scanPackage(QuickAPIConfig.controllerPackageNameList.toArray(new String[0]));
        List<APIController> apiControllerList = new ArrayList<>();

        for(Class _class:classList){
            String baseUrl = "";
            RequestMapping classRequestMapping = (RequestMapping) _class.getDeclaredAnnotation(RequestMapping.class);
            if(classRequestMapping!=null){
                baseUrl = classRequestMapping.value()[0];
            }
            APIController apiController = new APIController();
            //处理apiController
            {
                Deprecated deprecated = (Deprecated) _class.getDeclaredAnnotation(Deprecated.class);
                if(deprecated!=null){
                    apiController.deprecated = true;
                }
                apiController.className = _class.getName();
                apiController.tag = _class.getSimpleName();
            }
            //处理api
            List<API> apiList = new ArrayList<>();
            for(Method method:_class.getDeclaredMethods()){
                API api = new API();
                api.methodName = method.getName();
                api.brief = api.methodName;
                //deprecate
                {
                    Deprecated deprecated = method.getDeclaredAnnotation(Deprecated.class);
                    if(deprecated!=null){
                        api.deprecated = true;
                    }
                }
                //处理请求方法和映射路径
                handleRequestMapping(method,api,baseUrl);
                if(api.url==null){
                    continue;
                }
                //处理请求参数
                api.apiParameters = handleParameter(api,method);
                handleReturnValue(api,method);
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

    /**提取请求路径*/
    private static void handleRequestMapping(Method method,API api,String baseUrl) {
        for(Class _class:mappingClasses){
            Annotation annotation = method.getDeclaredAnnotation(_class);
            if(annotation==null){
                continue;
            }
            String requestMethod = _class.getSimpleName().substring(0,_class.getSimpleName().lastIndexOf("Mapping"));
            api.methods = new String[]{requestMethod};
            try {
                String[] values = (String[]) _class.getDeclaredMethod("value").invoke(annotation);
                api.url = baseUrl+values[0];
            }catch (Exception e){
                continue;
            }
            return;
        }
        RequestMapping methodRequestMapping = method.getDeclaredAnnotation(RequestMapping.class);
        if(methodRequestMapping==null){
            return;
        }
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
    }

    /**提取请求参数相关信息*/
    private static APIParameter[] handleParameter(API api,Method method){
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = u.getParameterNames(method);
        List<APIParameter> apiParameterList = new ArrayList<>();
        for(int i=0;i<parameters.length;i++){
            Class parameterType = parameters[i].getType();
            //排除特定类型的参数
            if(parameterType.getName().startsWith("javax.servlet")){
                continue;
            }
            //处理复杂对象
            if(PackageUtil.isInEntityPackage(parameterType.getName())){
                for(APIField apiField:apiEntityMap.get(parameterType.getName()).apiFields){
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
    private static void handleReturnValue(API api,Method method){
        api.returnValue = method.getGenericReturnType().getTypeName();
        Set<APIEntity> apiEntitySet = new LinkedHashSet<>();
        handleReturnEntity(method.getReturnType().getName(),apiEntitySet);
        //处理泛型
        {
            Type genericReturnType = method.getGenericReturnType();
            if(genericReturnType instanceof ParameterizedType){
                Type[] types = ((ParameterizedType)genericReturnType).getActualTypeArguments();
                for(Type type:types){
                    handleReturnEntity(type.getTypeName(),apiEntitySet);
                }
            }
        }
        api.returnEntityList = apiEntitySet.toArray(new APIEntity[0]);
    }

    /**处理返回类实体*/
    private static void handleReturnEntity(String className, Set<APIEntity> apiEntitySet){
        if(!apiEntityMap.containsKey(className)){
            return;
        }
        Stack<APIEntity> apiEntityStack = new Stack<>();
        apiEntityStack.push(apiEntityMap.get(className));
        while(!apiEntityStack.isEmpty()){
            APIEntity apiEntity = apiEntityStack.pop();
            apiEntitySet.add(apiEntity);
            for(APIField apiField:apiEntity.apiFields){
                APIEntity fieldEntity = apiEntityMap.get(apiField.className);
                if(fieldEntity!=null&&!hasRecycleDependency(fieldEntity,apiEntity)){
                    apiEntityStack.push(fieldEntity);
                }
            }
        }
    }

    private static boolean hasRecycleDependency(APIEntity fieldEntity,APIEntity parentEntity){
        for(APIField apiField:fieldEntity.apiFields){
            APIEntity subFieldEntity = apiEntityMap.get(apiField.className);
            if(subFieldEntity!=null&&subFieldEntity==parentEntity){
                return true;
            }
        }
        return false;
    }
}
