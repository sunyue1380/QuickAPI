package cn.schoolwow.quickapi.handler.controller;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.handler.entity.AbstractEntityHandler;
import cn.schoolwow.quickapi.util.JavaDocReader;
import cn.schoolwow.quickapi.util.PackageUtil;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public abstract class AbstractControllerHandler implements ControllerHandler{
    protected static Map<String,APIEntity> apiEntityMap = AbstractEntityHandler.apiEntityMap;
    public static List<APIController> apiControllerList = getApiControllerList();

    /**获取控制器列表*/
    private static List<APIController> getApiControllerList(){
        ControllerHandlerMapping[] controllerHandlerMappings = ControllerHandlerMapping.values();
        for(ControllerHandlerMapping controllerHandlerMapping:controllerHandlerMappings){
            try {
                Class.forName(controllerHandlerMapping.className);
                AbstractControllerHandler controllerHandler = (AbstractControllerHandler) controllerHandlerMapping._class.newInstance();
                return controllerHandler.getAPIList();
            } catch (ClassNotFoundException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        throw new UnsupportedOperationException("不支持的Controller层环境!");
    }

    private List<APIController> getAPIList(){
        List<Class> classList = PackageUtil.scanPackage(QuickAPIConfig.controllerPackageNameList.toArray(new String[0]));
        List<APIController> apiControllerList = new ArrayList<>();

        for(Class _class:classList){
            String baseUrl = getBaseUrl(_class);
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
                handleRequestMapping(method,api);
                if(null==api.url){
                    continue;
                }else{
                    api.url = baseUrl+api.url;
                }
                //处理请求参数
                api.apiParameters = handleParameter(method,api);
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
                            apiController.tag = classDoc.commentText().trim();
                        }
                        //获取brief和参数信息
                        {
                            MethodDoc[] methodDocs = classDoc.methods();
                            for(API api:apiController.apiList){
                                for(MethodDoc methodDoc:methodDocs){
                                    if(api.methodName.equals(methodDoc.name())){
                                        api.brief = methodDoc.commentText().trim();
                                        api.description = methodDoc.commentText().trim();
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

    protected abstract String getBaseUrl(Class _class);

    protected abstract void handleRequestMapping(Method method, API api);

    protected abstract APIParameter[] handleParameter(Method method, API api);

    /**提取请求参数相关信息*/
    protected void handleReturnValue(API api, Method method){
        api.returnValue = method.getGenericReturnType().getTypeName();
        Set<String> apiEntitySet = getRecycleEntity(method.getReturnType().getName());
        //处理泛型
        {
            Type genericReturnType = method.getGenericReturnType();
            if(genericReturnType instanceof ParameterizedType){
                Type[] types = ((ParameterizedType)genericReturnType).getActualTypeArguments();
                for(Type type:types){
                    apiEntitySet.addAll(getRecycleEntity(type.getTypeName()));
                }
            }
        }
        api.returnEntityNameList = apiEntitySet.toArray(new String[0]);
    }

    /**处理返回类实体*/
    protected Set<String> getRecycleEntity(String className){
        Set<String> apiEntitySet = new LinkedHashSet<>();
        if(!apiEntityMap.containsKey(className)){
            return apiEntitySet;
        }
        Stack<String> apiEntityStack = new Stack<>();
        apiEntityStack.push(className);
        while(!apiEntityStack.isEmpty()){
            String entityClassName = apiEntityStack.pop();
            APIEntity apiEntity = apiEntityMap.get(entityClassName);
            if(null==apiEntity){
                continue;
            }
            apiEntitySet.add(entityClassName);
            for(APIField apiField:apiEntity.apiFields){
                if(null==apiField.className){
                    continue;
                }
                String fieldClassName = apiField.className;
                if(fieldClassName.startsWith("[L")){
                    fieldClassName = fieldClassName.substring(2,fieldClassName.length()-1);
                }
                //TODO 处理List的类型
                if(!hasRecycleDependency(apiEntityMap.get(fieldClassName),apiEntity)){
                    apiEntityStack.push(fieldClassName);
                }
            }
        }
        return apiEntitySet;
    }

    private boolean hasRecycleDependency(APIEntity fieldEntity,APIEntity parentEntity){
        if(null==fieldEntity){
            return false;
        }
        for(APIField apiField:fieldEntity.apiFields){
            APIEntity subFieldEntity = apiEntityMap.get(apiField.className);
            if(subFieldEntity!=null&&subFieldEntity==parentEntity){
                return true;
            }
        }
        return false;
    }

}
