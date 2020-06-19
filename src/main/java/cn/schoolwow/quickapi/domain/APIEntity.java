package cn.schoolwow.quickapi.domain;

public class APIEntity {
    /**实体简写类名*/
    public String simpleName;
    /**实体类名*/
    public String className;
    /**描述*/
    private String description;
    /**作者*/
    public String author;
    /**日期*/
    public String since;
    /**实体字段列表*/
    public APIField[] apiFields;
    /**实体JSON字符串*/
    public String instance;
    /**实体类*/
    public transient Class clazz;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if(null==this.description||this.description.isEmpty()){
            this.description = description;
        }
    }
}
