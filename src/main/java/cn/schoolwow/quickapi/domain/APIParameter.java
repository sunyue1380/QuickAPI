package cn.schoolwow.quickapi.domain;

/**请求参数*/
public class APIParameter {
    /**参数名*/
    public String name;
    /**参数类型*/
    public String type;
    /**参数请求类型*/
    public String requestType = "text";
    /**参数位置(query,body)*/
    public String position = "body";
    /**描述*/
    public String description;
    /**是否必须*/
    public boolean required = true;
    /**默认值*/
    public String defaultValue = "";
    /**样例*/
    public String exampleEntity;
}
