package cn.schoolwow.quickapi.handler;

import cn.schoolwow.quickapi.domain.API;
import cn.schoolwow.quickapi.domain.APIController;
import cn.schoolwow.quickapi.domain.APIEntity;
import cn.schoolwow.quickapi.domain.APIMicroService;

public interface Handler {
    /**环境是否存在*/
    boolean exist();

    /**是否是控制器环境*/
    boolean isControllerEnvironment();

    /**获取控制器
     * @param clazz 类对象
     * */
    APIController getApiController(Class clazz, APIMicroService apiMicroService);

    /**
     * 处理控制器
     * @param apiController 控制器文档
     * */
    void handleController(APIController apiController);

    /**
     * 处理方法参数
     * @param api api文档
     * */
    void handleAPI(API api);

    /**
     * 处理实体类
     * @param apiEntity 实体类文档
     * */
    void handleEntity(APIEntity apiEntity);
}
