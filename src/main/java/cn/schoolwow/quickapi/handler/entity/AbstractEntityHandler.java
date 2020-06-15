package cn.schoolwow.quickapi.handler.entity;

import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIField;
import cn.schoolwow.quickapi.util.FieldUtil;
import cn.schoolwow.quickapi.util.JavaDocReader;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

import static cn.schoolwow.quickapi.util.QuickAPIConfig.apiDocument;

public class AbstractEntityHandler implements EntityHandler{
    private Logger logger = LoggerFactory.getLogger(AbstractEntityHandler.class);
    /**
     * 处理实体包类
     * */
    public static void handleEntityMap(){
        EntityHandlerMapping[] entityHandlerMappings = EntityHandlerMapping.values();
        AbstractEntityHandler entityHandler = null;
        for(EntityHandlerMapping entityHandlerMapping:entityHandlerMappings){
            try {
                Class.forName(entityHandlerMapping.className);
                entityHandler = (AbstractEntityHandler) entityHandlerMapping._class.newInstance();
                break;
            } catch (ClassNotFoundException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(null==entityHandler){
            entityHandler = new AbstractEntityHandler();
        }
        entityHandler.handleEntity();
    }

    private void handleEntity() {
        ClassDoc[] classDocs = JavaDocReader.getJavaDoc(apiDocument.apiEntityMap.keySet());
        for (APIEntity apiEntity : apiDocument.apiEntityMap.values()) {
            try {
                Class clazz = ClassLoader.getSystemClassLoader().loadClass(apiEntity.className);
                handleClass(clazz,apiEntity);
                for(Field field: FieldUtil.getAllField(clazz)){
                    field.setAccessible(true);
                    for(APIField apiField:apiEntity.apiFields){
                        if(apiField.name.equals(field.getName())){
                            handleField(field,apiField);
                            break;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                logger.warn("[类不存在]类名:{}",apiEntity.className);
                continue;
            }
            for (ClassDoc classDoc : classDocs) {
                if (apiEntity.className.equals(classDoc.qualifiedName())) {
                    if(apiEntity.description==null){
                        apiEntity.description = classDoc.commentText();
                    }
                    Tag[] authorTags = classDoc.tags("author");
                    if(null!=authorTags&&authorTags.length>0){
                        apiEntity.author = authorTags[0].text();
                    }
                    Tag[] sinceTags = classDoc.tags("since");
                    if(null!=sinceTags&&sinceTags.length>0){
                        apiEntity.since = sinceTags[0].text();
                    }
                    for (APIField apiField : apiEntity.apiFields) {
                        for (FieldDoc fieldDoc : FieldUtil.getAllFieldDoc(classDoc)) {
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

    /**处理实体类*/
    protected void handleClass(Class clazz, APIEntity apiEntity){

    }

    /**处理实体属性*/
    protected void handleField(Field field, APIField apiField){

    }

    private enum EntityHandlerMapping {
        QuickServer("cn.schoolwow.quickdao.dao.DAO",QuickDAOEntityHandler.class),
        Swagger("io.swagger.annotations.ApiModel",SwaggerEntityHandler.class);
        public String className;
        public Class _class;

        EntityHandlerMapping(String className, Class _class) {
            this.className = className;
            this._class = _class;
        }
    }
}
