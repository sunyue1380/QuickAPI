package cn.schoolwow.quickapi.handler.entity;

import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIField;
import cn.schoolwow.quickdao.annotation.Comment;
import cn.schoolwow.quickdao.annotation.Ignore;

import java.lang.reflect.Field;

public class QuickDAOEntityHandler extends AbstractEntityHandler{
    @Override
    protected void handleClass(Class clazz, APIEntity apiEntity) {
        Comment comment = (Comment) clazz.getAnnotation(Comment.class);
        if(null!=comment){
            apiEntity.description = comment.value();
        }
    }

    @Override
    protected void handleField(Field field, APIField apiField) {
        Comment comment = field.getAnnotation(Comment.class);
        if (null!=comment) {
            apiField.description = comment.value();
        }
        if (null != field.getAnnotation(Ignore.class)) {
            apiField.ignore = true;
        }
    }
}
