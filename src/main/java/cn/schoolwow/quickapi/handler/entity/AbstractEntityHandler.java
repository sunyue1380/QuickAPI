package cn.schoolwow.quickapi.handler.entity;

import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIField;
import cn.schoolwow.quickapi.util.JavaDocReader;
import cn.schoolwow.quickapi.util.PackageUtil;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractEntityHandler implements EntityHandler{
    public static Map<String,APIEntity> apiEntityMap = getEntityMap();
    /**获取实体类映射*/
    private static Map<String,APIEntity> getEntityMap(){
        List<Class> classList = PackageUtil.scanPackage(QuickAPIConfig.entityPackageNameList.toArray(new String[0]));
        EntityHandlerMapping[] entityHandlerMappings =EntityHandlerMapping.values();
        for(EntityHandlerMapping entityHandlerMapping:entityHandlerMappings){
            try {
                Class.forName(entityHandlerMapping.className);
                AbstractEntityHandler entityHandler = (AbstractEntityHandler) entityHandlerMapping._class.newInstance();
                return entityHandler.handleEntity(classList);
            } catch (ClassNotFoundException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        throw new UnsupportedOperationException("不支持的Entity层环境!");
    }

    private Map<String, APIEntity> handleEntity(List<Class> classList) {
        Map<String,APIEntity> apiEntityMap = new HashMap<>();
        for(Class _class:classList) {
            APIEntity apiEntity = new APIEntity();
            //处理类名
            {
                apiEntity.className = _class.getName();
                apiEntity.simpleName = _class.getSimpleName();
                handleClass(_class,apiEntity);
            }
            //处理字段
            List<Field> fieldList = new ArrayList<>();
            Class tempClass =_class;
            while(null!=tempClass){
                Field[] fields = tempClass.getDeclaredFields();
                //排除静态变量和常量
                Field.setAccessible(fields,true);
                for(Field field:fields){
                    if(Modifier.isFinal(field.getModifiers())||Modifier.isStatic(field.getModifiers())){
                        continue;
                    }
                    fieldList.add(field);
                }
                tempClass = tempClass.getSuperclass();
                if("java.lang.Object".equals(tempClass)){
                    break;
                }
            }
            Field[] fields = fieldList.toArray(new Field[0]);
            APIField[] apiFields = new APIField[fields.length];
            {
                Field.setAccessible(fields, true);
                for (int i = 0; i < fields.length; i++) {
                    APIField apiField = new APIField();
                    apiField.name = fields[i].getName();
                    apiField.className = fields[i].getType().getName();
                    handleField(fields[i],apiField);
                    apiFields[i] = apiField;
                }
            }
            apiEntity.apiFields = apiFields;
            apiEntityMap.put(_class.getName(), apiEntity);
        }
        //提取注释部分
        {
            ClassDoc[] classDocs = JavaDocReader.getEntityJavaDoc();
            for (APIEntity apiEntity : apiEntityMap.values()) {
                for (ClassDoc classDoc : classDocs) {
                    if (apiEntity.className.equals(classDoc.qualifiedName())) {
                        if(apiEntity.description==null){
                            apiEntity.description = classDoc.getRawCommentText();
                        }
                        for (APIField apiField : apiEntity.apiFields) {
                            for (FieldDoc fieldDoc : classDoc.fields()) {
                                if (apiField.name.equals(fieldDoc.name())&&apiField.description==null) {
                                    apiField.description = fieldDoc.getRawCommentText();
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        return apiEntityMap;
    }

    /**处理实体类*/
    public abstract void handleClass(Class _class,APIEntity apiEntity);

    /**处理实体属性*/
    public abstract void handleField(Field field, APIField apiField);
}
