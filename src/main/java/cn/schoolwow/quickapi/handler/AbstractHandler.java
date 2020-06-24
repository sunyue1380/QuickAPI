package cn.schoolwow.quickapi.handler;


import cn.schoolwow.quickapi.domain.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import static cn.schoolwow.quickapi.util.QuickAPIUtil.getRecycleEntity;

public abstract class AbstractHandler implements Handler {
    private Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

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
}
