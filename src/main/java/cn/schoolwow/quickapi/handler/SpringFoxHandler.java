package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Iterator;

public class SpringFoxHandler extends AbstractHandler{
    @Override
    public boolean exist() {
        try {
            QuickAPIConfig.urlClassLoader.loadClass("springfox.documentation.annotations.ApiIgnore");
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
    public APIController getApiController(Class clazz, APIMicroService apiMicroService) {
        return null;
    }

    @Override
    public void handleController(APIController apiController) {
        for(API api:apiController.apiList){
            Iterator<APIParameter> iterator = api.apiParameters.iterator();
            while(iterator.hasNext()){
                APIParameter apiParameter = iterator.next();
                ApiIgnore apiIgnore = apiParameter.parameter.getAnnotation(ApiIgnore.class);
                if(null!=apiIgnore){
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void handleAPI(API api) {

    }

    @Override
    public void handleEntity(APIEntity apiEntity) {
    }
}
