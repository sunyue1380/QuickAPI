package cn.schoolwow.quickapi.handler;


import cn.schoolwow.quickapi.domain.API;
import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIField;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import cn.schoolwow.quickapi.util.QuickAPIUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public abstract class AbstractHandler implements Handler {
    private Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

    /**
     * 是否需要过滤该类
     * @param className 类名
     * */
    protected boolean needIgnoreClass(String className){
        for(String ignorePackageName: QuickAPIConfig.ignorePackageNameList){
            if(className.startsWith(ignorePackageName)){
                return true;
            }
        }
        return false;
    }

    protected void handleReturnValue(API api) {
        Method method = api.method;
        api.returnValue = method.getGenericReturnType().getTypeName();
        if(method.getReturnType().isPrimitive()){
            return;
        }
        Set<String> apiEntitySet = getRecycleEntity(api.returnValue);
        //处理泛型
        {
            Type genericReturnType = method.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType) {
                Type[] types = ((ParameterizedType) genericReturnType).getActualTypeArguments();
                for (Type type : types) {
                    apiEntitySet.addAll(getRecycleEntity(type.getTypeName()));
                }
            }
        }
        api.returnEntityNameList = apiEntitySet.toArray(new String[0]);
    }

    protected Set<String> getRecycleEntity(String className) {
        Set<String> apiEntitySet = new LinkedHashSet<>();
        if(needIgnoreClass(className)){
            return apiEntitySet;
        }
        Stack<String> apiEntityStack = new Stack<>();
        apiEntityStack.push(className);
        while (!apiEntityStack.isEmpty()) {
            Class clazz = null;
            try {
                clazz = QuickAPIConfig.urlClassLoader.loadClass(apiEntityStack.pop());
            } catch (ClassNotFoundException e) {
                logger.warn("[加载类不存在]类名:{}", className);
                continue;
            }
            apiEntitySet.add(clazz.getName());
            Map<String, APIEntity> apiEntityMap = QuickAPIConfig.apiDocument.apiEntityMap;
            if (apiEntityMap.containsKey(clazz.getName())) {
                continue;
            }
            APIEntity apiEntity = addAPIEntity(clazz);
            apiEntityMap.put(clazz.getName(), apiEntity);
            for (APIField apiField : apiEntity.apiFields) {
                if (apiField.className.startsWith("[L")) {
                    apiField.className = apiField.className.substring(2, apiField.className.length() - 1);
                } else if (apiField.className.contains("<") && apiField.className.contains(">")) {
                    apiField.className = apiField.className.substring(apiField.className.indexOf("<") + 1, apiField.className.indexOf(">"));
                }

                if(needIgnoreClass(apiField.className)){
                    continue;
                }
                if (!apiEntityMap.containsKey(apiField.className)) {
                    apiEntityStack.push(apiField.className);
                }
            }
        }
        return apiEntitySet;
    }

    private APIEntity addAPIEntity(Class clazz) {
        APIEntity apiEntity = new APIEntity();
        //处理类名
        {
            apiEntity.clazz = clazz;
            apiEntity.className = clazz.getName();
            apiEntity.simpleName = clazz.getSimpleName();
        }
        //处理Field
        {
            Field[] fields = QuickAPIUtil.getAllField(clazz);
            APIField[] apiFields = new APIField[fields.length];
            Field.setAccessible(fields, true);
            for (int i = 0; i < fields.length; i++) {
                APIField apiField = new APIField();
                apiField.field = fields[i];
                apiField.name = fields[i].getName();
                apiField.className = fields[i].getType().getName();
                //处理泛型
                Type type = fields[i].getGenericType();
                if (type instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType) type;
                    Type genericType = pType.getActualTypeArguments()[0];
                    apiField.className += "<" + genericType.getTypeName() + ">";
                }
                apiFields[i] = apiField;
            }
            apiEntity.apiFields = apiFields;
        }
        try {
            apiEntity.instance = JSON.toJSONString(clazz.newInstance(), SerializerFeature.WriteMapNullValue);
        } catch (Exception e) {
            logger.warn("[实例化失败]原因:{},类名:{}",e.getMessage(),clazz.getName());
        }
        return apiEntity;
    }
}
