package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.util.JavaDocReader;
import cn.schoolwow.quickapi.util.PackageUtil;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public abstract class AbstractControllerHandler implements ControllerHandler{
    protected static Map<String,APIEntity> apiEntityMap = EntityHandler.getEntityList();

    @Override
    public abstract String getBaseUrl(Class _class);

    @Override
    public abstract void handleRequestMapping(Method method, API api);

    @Override
    public abstract APIParameter[] handleParameter(Method method, API api);

    public List<APIController> getAPIList(){
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
    protected void handleReturnValue(API api, Method method){
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
    protected void handleReturnEntity(String className, Set<APIEntity> apiEntitySet){
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

    private boolean hasRecycleDependency(APIEntity fieldEntity,APIEntity parentEntity){
        for(APIField apiField:fieldEntity.apiFields){
            APIEntity subFieldEntity = apiEntityMap.get(apiField.className);
            if(subFieldEntity!=null&&subFieldEntity==parentEntity){
                return true;
            }
        }
        return false;
    }

}
