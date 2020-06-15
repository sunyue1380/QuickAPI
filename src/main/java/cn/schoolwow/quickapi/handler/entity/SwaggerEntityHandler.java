package cn.schoolwow.quickapi.handler.entity;

import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.lang.reflect.Field;

public class SwaggerEntityHandler extends AbstractEntityHandler{
    @Override
    public void handleClass(Class clazz, APIEntity apiEntity) {
        ApiModel apiModel = (ApiModel) clazz.getAnnotation(ApiModel.class);
        if (null!=apiModel) {
            apiEntity.description = apiModel.description();
            if(null==apiEntity.description|| apiEntity.description.isEmpty()){
                apiEntity.description = apiModel.value();
            }
        }
    }

    @Override
    public void handleField(Field field, APIField apiField) {
        ApiModelProperty apiModelProperty = field.getAnnotation(ApiModelProperty.class);
        if (null!=apiModelProperty) {
            apiField.description = apiModelProperty.value();
        }
    }
}
