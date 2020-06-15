package cn.schoolwow.quickapi.controller;

import cn.schoolwow.quickdao.annotation.Comment;

public class User {
    /**用户名*/
    private String username;
    /**密码*/
    private String password;
    @Comment("年龄")
    private Integer age;
    /**数组*/
    private Long[] numbers;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Long[] getNumbers() {
        return numbers;
    }

    public void setNumbers(Long[] numbers) {
        this.numbers = numbers;
    }
}
