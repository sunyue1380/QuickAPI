package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIField;
import cn.schoolwow.quickapi.util.JavaDocReader;
import cn.schoolwow.quickapi.util.PackageUtil;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import cn.schoolwow.quickdao.annotation.Comment;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityHandler {
    /**
     * 扫描实体类信息
     */
    public static Map<String,APIEntity> getEntityList(){
        List<Class> classList = PackageUtil.scanPackage(QuickAPIConfig.entityPackageNameList.toArray(new String[0]));
        Map<String,APIEntity> apiEntityMap = new HashMap<>();
        for(Class _class:classList){
            APIEntity apiEntity = new APIEntity();
            //处理类名
            {
                apiEntity.className = _class.getName();
                apiEntity.simpleName = _class.getSimpleName();
                if(QuickAPIConfig.existQuickDAO){
                    Comment comment = (Comment) _class.getAnnotation(Comment.class);
                    if(comment!=null){
                        apiEntity.description = comment.value();
                    }
                }
            }
            //处理字段
            Field[] fields = _class.getDeclaredFields();
            APIField[] apiFields = new APIField[fields.length];
            {
                Field.setAccessible(fields,true);
                for(int i=0;i<fields.length;i++){
                    APIField apiField = new APIField();
                    apiField.name = fields[i].getName();
                    apiField.className = fields[i].getType().getName();
                    if(QuickAPIConfig.existQuickDAO){
                        Comment comment = fields[i].getAnnotation(Comment.class);
                        if(comment!=null){
                            apiField.description = comment.value();
                        }
                    }
                    apiFields[i] = apiField;
                }
            }
            apiEntity.apiFields = apiFields;
            apiEntityMap.put(_class.getName(),apiEntity);
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
}
