package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import io.swagger.annotations.*;

public class SwaggerHandler extends AbstractHandler{

    @Override
    public boolean exist() {
        try {
            QuickAPIConfig.urlClassLoader.loadClass("io.swagger.annotations.ApiModel");
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
        //API标签
        {
            Api api = (Api) apiController.clazz.getAnnotation(API.class);
            if(null!=api){
                apiController.name = api.tags()[0];
            }
        }
        for(API api:apiController.apiList){
            ApiOperation apiOperation = api.method.getAnnotation(ApiOperation.class);
            if(null!=apiOperation){
                api.name = apiOperation.value();
                api.description = apiOperation.notes();
            }

            for(APIParameter apiParameter:api.apiParameters){
                ApiParam apiParam = apiParameter.parameter.getAnnotation(ApiParam.class);
                if(null!=apiParam){
                    if(apiParameter.name.isEmpty()){
                        apiParameter.name = apiParam.name();
                    }
                    apiParameter.description = apiParam.value();
                    apiParameter.required = apiParam.required();
                    apiParameter.defaultValue = apiParam.defaultValue();
                }
            }
        }
    }

    @Override
    public void handleAPI(API api) {

    }

    @Override
    public void handleEntity(APIEntity apiEntity) {
        {
            ApiModel apiModel = (ApiModel) apiEntity.clazz.getAnnotation(ApiModel.class);
            if (null!=apiModel) {
                apiEntity.description = apiModel.description();
                if(null==apiEntity.description|| apiEntity.description.isEmpty()){
                    apiEntity.description = apiModel.value();
                }
            }
        }
        {
            for(APIField apiField:apiEntity.apiFields){
                ApiModelProperty apiModelProperty = apiField.field.getAnnotation(ApiModelProperty.class);
                if (null!=apiModelProperty) {
                    apiField.description = apiModelProperty.value();
                    apiField.required = apiModelProperty.required();
                    apiField.example = apiModelProperty.example();
                }
            }
        }
    }
}
