package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.API;
import cn.schoolwow.quickapi.domain.APIController;
import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIField;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import cn.schoolwow.quickdao.annotation.Comment;
import cn.schoolwow.quickdao.annotation.Constraint;
import cn.schoolwow.quickdao.annotation.Ignore;

public class QuickDAOHandler extends AbstractHandler{

    @Override
    public boolean exist() {
        try {
            QuickAPIConfig.urlClassLoader.loadClass("cn.schoolwow.quickdao.dao.DAO");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean isControllerEnvironment() {
        return false;
    }

    @Override
    public APIController getApiController(Class clazz) {
        return null;
    }

    @Override
    public void handleController(APIController apiController) {

    }

    @Override
    public void handleAPI(API api) {

    }

    @Override
    public void handleEntity(APIEntity apiEntity) {
        {
            Comment comment = (Comment) apiEntity.clazz.getAnnotation(Comment.class);
            if(null!=comment){
                apiEntity.setDescription(comment.value());
            }
        }
        {
            for(APIField apiField:apiEntity.apiFields){
                Comment comment = apiField.field.getAnnotation(Comment.class);
                if (null!=comment) {
                    apiField.setDescription(comment.value());
                }
                if (null != apiField.field.getAnnotation(Ignore.class)) {
                    apiField.ignore = true;
                }
                Constraint constraint = apiField.field.getAnnotation(Constraint.class);
                if(null!=constraint){
                    apiField.required = constraint.notNull();
                    apiField.example = constraint.defaultValue();
                }
            }
        }
    }
}
