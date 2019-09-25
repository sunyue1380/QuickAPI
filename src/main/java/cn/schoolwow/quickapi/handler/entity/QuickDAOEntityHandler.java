package cn.schoolwow.quickapi.handler.entity;

import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIField;
import cn.schoolwow.quickdao.annotation.Comment;
import cn.schoolwow.quickdao.annotation.Ignore;

import java.lang.reflect.Field;

public class QuickDAOEntityHandler extends AbstractEntityHandler{
    @Override
    public void handleClass(Class _class, APIEntity apiEntity) {
        Comment comment = (Comment) _class.getAnnotation(Comment.class);
            apiEntity.description = comment.value();
    }

    @Override
    public void handleField(Field field, APIField apiField) {
        Comment comment = field.getAnnotation(Comment.class);
        if (comment != null) {
            apiField.description = comment.value();
        }
        if (null != field.getAnnotation(Ignore.class)) {
            apiField.ignore = true;
        }
    }
}
