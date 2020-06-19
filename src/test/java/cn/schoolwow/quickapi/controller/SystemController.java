package cn.schoolwow.quickapi.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/upload")
    public void upload(
            @ApiParam(value = "用户扩展信息",required = true) MultipartFile file
    ){
        logger.info("[上传文件]参数名:{},文件名:{},文件大小:{}",file.getName(),file.getOriginalFilename(),file.getSize());
    }
}
