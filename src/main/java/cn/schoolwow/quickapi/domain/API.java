package cn.schoolwow.quickapi.domain;

public class API {
    /**是否被废弃*/
    public boolean deprecated;
    /**请求方法*/
    public String[] methods;
    /**请求地址*/
    public String url;
    /**名称*/
    public String brief;
    /**描述*/
    public String description;
    /**方法名*/
    public String methodName;
    /**请求编码*/
    public String contentType = "application/x-www-form-urlencoded";
    /**请求参数*/
    public APIParameter[] apiParameters;
    /**返回值*/
    public String returnValue;
    /**返回实体类信息*/
    public APIEntity[] returnEntityList;
}
