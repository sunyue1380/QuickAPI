package cn.schoolwow.quickapi.controller;

import cn.schoolwow.quickapi.entity.Teacher;
import cn.schoolwow.quickapi.entity.User;
import cn.schoolwow.quickdao.domain.PageVo;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @tag 首页
 * */
@RequestMapping("/index")
@Controller
public class IndexController {
    private Logger logger = LoggerFactory.getLogger(IndexController.class);
    /**
     * @brief 用户注册
     * @param username 用户名
     * @param password 密码
     * */
    @RequestMapping("/register")
    @ResponseBody
    public String register(
            @RequestParam("username") String username,
            String password
    ){
        logger.info("[用户注册]用户名:{},密码:{}",username,password);
        return "注册成功!用户名:"+username+",密码:"+password;
    }

    /**
     * @brief 用户登录
     * @param username 用户名
     * @param password 密码
     * */
    @RequestMapping("/login")
    @ResponseBody
    public User login(
            @RequestParam("username") String username,
            String password
    ){
        logger.info("[用户登录]用户名:{},密码:{}",username,password);
        User user = new User();
        user.setUsername("quickapi");
        user.setPassword("123456");
        return user;
    }

    /**
     * @brief 上传
     * @param multipartFile 要上传的文件
     * */
    @RequestMapping("/upload")
    @ResponseBody
    public String upload(
            @RequestPart(value = "file",required = false) MultipartFile multipartFile
    ){
        logger.info("[上传文件]{}",multipartFile);
        return multipartFile.getOriginalFilename();
    }

    /**
     * @brief 用户查询
     * @param user 查询条件
     * */
    @RequestMapping("/query")
    @ResponseBody
    public String query(
            @RequestBody User user
    ){
        logger.info("[JSON格式查询]{}",user);
        return JSON.toJSONString(user);
    }

    /**
     * @brief Restful查询
     * @param userId 查询条件
     * */
    @RequestMapping("/user/{userId}")
    @ResponseBody
    public String restful(
            @PathVariable long userId
    ){
        logger.info("[restful]userId:{}",userId);
        return userId+"";
    }

    /**
     * @brief 泛型查询
     * */
    @RequestMapping("/parameterized")
    @ResponseBody
    public Teacher<User> parameterized(

    ){
        return new Teacher<>();
    }
}
