package cn.schoolwow.quickapi.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;

/**
 * @apiNote 系统服务
 * */
@RestController
@RequestMapping("/system")
public class SystemController {
    private Logger logger = LoggerFactory.getLogger(SystemController.class);


    /**
     * 忘记密码
     * @param user 用户
     * */
    @PostMapping(value = "/updatePassword")
    public boolean updatePassword(
            @RequestBody User user
    ){
        logger.info("[更新密码]用户名:{},密码:{}",user.getUsername(),user.getPassword());
        return true;
    }

    /**
     * 更新用户信息
     * @param user 用户
     * */
    @PutMapping(value = "/updateUser")
    public Object updateUser(
            @RequestBody User user
    ){
        return true;
    }

    @ApiOperation(value = "更新用户扩展信息",notes = "更新用户扩展信息")
    @PutMapping(value = "/updatePassword")
    public Object updateUserExtend(
            @ApiParam(name = "body",value = "用户扩展信息",required = true)
            @RequestBody UserExtend userExtend
    ){
        return true;
    }

    /**
     * 忽略参数
     * @param user 用户
     * */
    @PostMapping(value = "/ignore")
    public void upload(
            @ApiParam(value = "文件",required = true) MultipartFile file,
            @ApiIgnore User user,
            HttpServletResponse response
    ){}

    /**
     * 表单提交
     * @param user 用户
     * */
    @PostMapping(value = "/updateUser")
    public void upload(
            User user
    ){
        logger.info("[用户值]用户名:{},密码:{}",user.getUsername(),user.getPassword());
    }
}
