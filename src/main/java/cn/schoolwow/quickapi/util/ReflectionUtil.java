package cn.schoolwow.quickapi.util;

import cn.schoolwow.quickapi.domain.API;
import cn.schoolwow.quickapi.domain.APIController;
import cn.schoolwow.quickapi.domain.APIParameter;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionUtil {
    private static Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);
    private static LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();

    /**扫描控制类提取接口信息*/
    public static List<APIController> getAPIList(List<Class> classList){
        List<APIController> apiControllerList = new ArrayList<>();
        for(Class _class:classList){
            String baseUrl = "";
            RequestMapping classRequestMapping = (RequestMapping) _class.getDeclaredAnnotation(RequestMapping.class);
            if(classRequestMapping!=null){
                baseUrl = classRequestMapping.value()[0];
            }
            APIController apiController = new APIController();
            apiController.className = _class.getSimpleName();
            apiController.tag = _class.getSimpleName();

            List<API> apiList = new ArrayList<>();
            for(Method method:_class.getDeclaredMethods()){
                RequestMapping methodRequestMapping = method.getDeclaredAnnotation(RequestMapping.class);
                if(methodRequestMapping==null){
                    continue;
                }
                API api = new API();
                api.methodName = method.getName();
                api.brief = api.methodName;
                //处理请求方法
                RequestMethod[] requestMethods = methodRequestMapping.method();
                if(requestMethods.length>0){
                    api.methods = new String[requestMethods.length];
                    for(int i=0;i<requestMethods.length;i++){
                        api.methods[i] = requestMethods[i].name().toUpperCase();
                    }
                }else{
                    api.methods = new String[]{"all"};
                }
                //处理请求路径
                api.url = baseUrl+methodRequestMapping.value()[0];
                //处理请求参数
                api.apiParameters = handleParameter(api,method);
                api.returnValue = method.getGenericReturnType().getTypeName();
                apiList.add(api);
            }
            if(apiList.size()==0){
                continue;
            }
            apiController.apiList = apiList;
            apiControllerList.add(apiController);
        }
        //处理注释
        {
            ClassDoc[] classDocs = JavaDocReader.extractJavaDoc();
            for(APIController apiController:apiControllerList){
                for(ClassDoc classDoc:classDocs){
                    if(apiController.className.equals(classDoc.name())){
                        //获取tag
                        {
                            Tag[] tags = classDoc.tags("tag");
                            if(tags!=null&&tags.length>0){
                                apiController.tag = tags[0].text();
                            }
                        }
                        //获取brief和参数信息
                        {
                            MethodDoc[] methodDocs = classDoc.methods();
                            for(API api:apiController.apiList){
                                for(MethodDoc methodDoc:methodDocs){
                                    if(api.methodName.equals(methodDoc.name())){
                                        Tag[] briefs = methodDoc.tags("brief");
                                        if(briefs!=null&&briefs.length>0){
                                            api.brief = briefs[0].text();
                                        }
                                        api.description = methodDoc.commentText();
                                        //获取参数信息
                                        ParamTag[] paramTags = methodDoc.paramTags();
                                        for(APIParameter apiParameter:api.apiParameters){
                                            for(ParamTag paramTag:paramTags){
                                                if(apiParameter.name.equals(paramTag.parameterName())){
                                                    apiParameter.name = paramTag.parameterName();
                                                    apiParameter.description = paramTag.parameterComment();
                                                    break;
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        return apiControllerList;
    }

//    private static JSONObject getReturnValue(Class _class){
//        JSONObject o = new JSONObject();
//        if(_class.isPrimitive()){
//        }
//        return null;
////        _class.getGenericSuperclass().getTypeName();
//    }

    /**提取请求参数相关信息*/
    private static APIParameter[] handleParameter(API api,Method method){
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = u.getParameterNames(method);
        List<APIParameter> apiParameterList = new ArrayList<>();
        for(int i=0;i<parameters.length;i++){
            //排除特定类型的参数
            if(parameters[i].getType().getName().startsWith("javax.servlet")){
                continue;
            }
            APIParameter apiParameter = new APIParameter();
            //RequestParam
            {
                RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
                if(requestParam!=null){
                    apiParameter.name = requestParam.value();
                    if(apiParameter.name.isEmpty()){
                        apiParameter.name = requestParam.name();
                    }
                    apiParameter.required = requestParam.required();
                    if(!requestParam.defaultValue().equals(ValueConstants.DEFAULT_NONE)){
                        apiParameter.defaultValue = requestParam.defaultValue();
                    }
                }
            }
            //RequestPart
            {
                RequestPart requestPart = parameters[i].getAnnotation(RequestPart.class);
                if(requestPart!=null){
                    apiParameter.name = requestPart.value();
                    if(apiParameter.name.isEmpty()){
                        apiParameter.name = requestPart.name();
                    }
                    apiParameter.required = requestPart.required();
                    apiParameter.requestType = "file";
                    api.contentType = "multipart/form-data;";
                }
            }
            //RequestBody
            {
                RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
                if(requestBody!=null){
                    apiParameter.requestType = "textarea";
                    api.contentType = "application/json; charset=utf-8";
                }
            }
            //PathVaribale
            {
                PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
                if(pathVariable!=null){
                    apiParameter.position = "query";
                }
            }
            if(apiParameter.name==null||apiParameter.name.isEmpty()){
                apiParameter.name = parameterNames[i];
                apiParameter.required = true;
            }
            apiParameter.type = parameters[i].getType().getName();
            apiParameterList.add(apiParameter);
        }
        return apiParameterList.toArray(new APIParameter[0]);
    }

    /**扫描用户指定包中的类*/
    public static List<Class> scanPackageList() throws Exception{
        List<Class> classList = new ArrayList<>();
        for(String packageName:QuickAPIConfig.packageNames){
            String packageNamePath = packageName.replace(".", "/");
            Enumeration<URL> urlEnumeration = Thread.currentThread().getContextClassLoader().getResources(packageNamePath);
            while(urlEnumeration.hasMoreElements()){
                URL url = urlEnumeration.nextElement();
                if(url==null){
                    continue;
                }
                switch (url.getProtocol()) {
                    case "file": {
                        File file = new File(url.getFile());
                        //TODO 对于有空格或者中文路径会无法识别
                        logger.info("[类文件路径]{}", file.getAbsolutePath());
                        if (!file.isDirectory()) {
                            throw new IllegalArgumentException("包名不是合法的文件夹!" + url.getFile());
                        }
                        Stack<File> stack = new Stack<>();
                        stack.push(file);
                        String indexOfString = packageName.replace(".", "/");
                        while (!stack.isEmpty()) {
                            file = stack.pop();
                            for (File f : file.listFiles()) {
                                if (f.isDirectory()) {
                                    stack.push(f);
                                } else if (f.isFile() && f.getName().endsWith(".class")) {
                                    String path = f.getAbsolutePath().replace("\\", "/");
                                    int startIndex = path.indexOf(indexOfString);
                                    String className = path.substring(startIndex, path.length() - 6).replace("/", ".");
                                    classList.add(Class.forName(className));
                                }
                            }
                        }
                    }
                    break;
                    case "jar": {
                        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                        if (null != jarURLConnection) {
                            JarFile jarFile = jarURLConnection.getJarFile();
                            if (null != jarFile) {
                                Enumeration<JarEntry> jarEntries = jarFile.entries();
                                while (jarEntries.hasMoreElements()) {
                                    JarEntry jarEntry = jarEntries.nextElement();
                                    String jarEntryName = jarEntry.getName();
                                    if (jarEntryName.contains(packageNamePath) && jarEntryName.endsWith(".class")) { //是否是类,是类进行加载
                                        String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                                        classList.add(Class.forName(className));
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        if (classList.size() == 0) {
            logger.warn("[扫描实体类信息为空]");
            return classList;
        }
        Stream<Class> stream = classList.stream().filter((_class)->{
            boolean result = true;
            //根据类过滤
            if(QuickAPIConfig.ignoreClassList!=null){
                if(QuickAPIConfig.ignoreClassList.contains(_class)){
                    logger.warn("[忽略类名]类名:{}!",_class.getName());
                    result = false;
                }
            }
            //根据包名过滤
            if(QuickAPIConfig.ignorePackageNameList!=null){
                for(String ignorePackageName:QuickAPIConfig.ignorePackageNameList){
                    if(_class.getName().contains(ignorePackageName)){
                        logger.warn("[忽略包名]包名:{}类名:{}",ignorePackageName,_class.getName());
                        result = false;
                    }
                }
            }
            return result;
        });
        if(QuickAPIConfig.predicate!=null){
            stream.filter(QuickAPIConfig.predicate);
        }
        classList = stream.collect(Collectors.toList());
        return classList;
    }
}
