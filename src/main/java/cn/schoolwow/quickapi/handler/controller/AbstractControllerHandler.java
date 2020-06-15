package cn.schoolwow.quickapi.handler.controller;

import cn.schoolwow.quickapi.domain.*;
import cn.schoolwow.quickapi.util.FieldUtil;
import cn.schoolwow.quickapi.util.JavaDocReader;
import cn.schoolwow.quickapi.util.QuickAPIConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sun.javadoc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class AbstractControllerHandler implements ControllerHandler {
    private static Logger logger = LoggerFactory.getLogger(AbstractControllerHandler.class);
    /**保存控制器类涉及的返回实体类*/
    private static final Map<String, APIEntity> apiEntityMap = new HashMap<>();
    /**保存请求参数*/
    protected final List<APIParameter> apiParameterList = new ArrayList<>();

    /**
     * 处理控制器类
     */
    public static void handleApiControllerList() {
        AbstractControllerHandler controllerHandler = null;
        try {
            if(null!=QuickAPIConfig.controllerHandlerMapping){
                controllerHandler = (AbstractControllerHandler) QuickAPIConfig.controllerHandlerMapping.clazz.newInstance();
            }else{
                ControllerHandlerMapping[] controllerHandlerMappings = ControllerHandlerMapping.values();
                for (ControllerHandlerMapping controllerHandlerMapping : controllerHandlerMappings) {
                    try {
                        Class.forName(controllerHandlerMapping.className);
                        controllerHandler = (AbstractControllerHandler) controllerHandlerMapping.clazz.newInstance();
                        break;
                    } catch (ClassNotFoundException e) {
                        logger.warn("[类不存在]{}",e.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if(null!=controllerHandler){
            QuickAPIConfig.apiDocument.apiEntityMap = apiEntityMap;
            QuickAPIConfig.apiDocument.apiControllerList = controllerHandler.getAPIList();
        }else{
            throw new UnsupportedOperationException("不支持的Controller层环境!");
        }
    }

    /**
     * 是否是合法的控制器类
     *
     * @param clazz 控制器类
     */
    protected abstract boolean isValidController(Class clazz);

    /**
     * 获取基础url
     *
     * @param clazz 控制器类
     */
    protected abstract String getBaseUrl(Class clazz);

    /**
     * 处理RequestMapping注解
     *
     * @param method 方法
     * @param api    api对象
     */
    protected abstract void handleRequestMapping(Method method, API api);

    /**
     * 处理方法参数
     *
     * @param method 方法
     * @param api    api对象
     */
    protected abstract APIParameter[] handleParameter(Method method, API api);

    /**是否需要过滤该类*/
    private boolean shouldFilterClassName(String className){
        //根据类过滤
        if(!QuickAPIConfig.ignoreClassList.isEmpty()){
            //为保证忽略内部类,需要以下处理
            for(String ignoreClassName:QuickAPIConfig.ignoreClassList){
                if(className.startsWith(ignoreClassName)){
                    logger.warn("[忽略类名]类名:{}!",className);
                    return true;
                }
            }
        }
        //根据包名过滤
        if(!QuickAPIConfig.ignorePackageNameList.isEmpty()){
            for(String ignorePackageName:QuickAPIConfig.ignorePackageNameList){
                if(className.contains(ignorePackageName)){
                    logger.warn("[忽略包名]包名:{}类名:{}",ignorePackageName,className);
                    return true;
                }
            }
        }
        if(null!=QuickAPIConfig.predicate&&QuickAPIConfig.predicate.test(className)){
            return true;
        }
        return false;
    }

    private List<APIController> getAPIList() {
        Set<String> classNameSet = new HashSet<>();
        for(String packageName:QuickAPIConfig.controllerPackageNameList.toArray(new String[0])){
            String packageNamePath = packageName.replace(".", "/");
            try {
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
                            String indexOfString = packageName.replace(".", "/");
                            Files.walkFileTree(file.toPath(),new SimpleFileVisitor<Path>(){
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                        throws IOException
                                {
                                    File f = file.toFile();
                                    if(f.getName().endsWith(".class")){
                                        String path = f.getAbsolutePath().replace("\\", "/");
                                        int startIndex = path.indexOf(indexOfString);
                                        String className = path.substring(startIndex, path.length() - 6).replace("/", ".");
                                        if(!shouldFilterClassName(className)){
                                            classNameSet.add(className);
                                        }
                                    }
                                    return FileVisitResult.CONTINUE;
                                }
                            });
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
                                        if (jarEntryName.contains(packageNamePath) && jarEntryName.endsWith(".class")) {
                                            String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                                            if(!shouldFilterClassName(className)){
                                                classNameSet.add(className);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        classNameSet.addAll(QuickAPIConfig.controllerClassNameList);

        List<APIController> apiControllerList = new ArrayList<>(classNameSet.size());
        for (String className : classNameSet) {
            Class clazz = null;
            try {
                clazz = ClassLoader.getSystemClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            }
            if(!isValidController(clazz)){
                continue;
            }
            APIController apiController = new APIController();
            //处理apiController
            {
                Deprecated deprecated = (Deprecated) clazz.getDeclaredAnnotation(Deprecated.class);
                if (deprecated != null) {
                    apiController.deprecated = true;
                }
                apiController.className = clazz.getName();
                apiController.name = clazz.getSimpleName();
            }
            //处理api
            List<API> apiList = new ArrayList<>();
            String baseUrl = getBaseUrl(clazz);
            for (Method method : clazz.getDeclaredMethods()) {
                API api = new API();
                api.methodName = method.getName();
                api.name = api.methodName;
                //deprecate
                {
                    Deprecated deprecated = method.getDeclaredAnnotation(Deprecated.class);
                    if (deprecated != null) {
                        api.deprecated = true;
                    }
                }
                //处理请求方法和映射路径
                handleRequestMapping(method, api);
                if (null == api.url) {
                    continue;
                } else {
                    api.url = baseUrl + api.url;
                }
                //处理请求参数
                api.apiParameters = handleParameter(method, api);
                api.actualParameterLength = method.getParameterCount();
                handleReturnValue(method, api);
                apiList.add(api);
            }
            if (apiList.size() == 0) {
                continue;
            }
            apiController.apiList = apiList;
            apiControllerList.add(apiController);
        }

        {
            //添加请求参数类名
            ClassDoc[] classDocs = JavaDocReader.getJavaDoc(classNameSet);
            for (APIController apiController : apiControllerList) {
                for (ClassDoc classDoc : classDocs) {
                    //判断控制器类名是否匹配
                    if (!apiController.className.equals(classDoc.qualifiedName())) {
                        continue;
                    }
                    String name = classDoc.commentText().trim();
                    if (!name.isEmpty()) {
                        apiController.name = name;
                    }
                    //匹配方法
                    MethodDoc[] methodDocs = classDoc.methods();
                    for (API api : apiController.apiList) {
                        for (MethodDoc methodDoc : methodDocs) {
                            //方法名和参数类型,个数匹配才算匹配
                            if(!api.methodName.equals(methodDoc.name())){
                                continue;
                            }
                            if(api.actualParameterLength!=methodDoc.parameters().length){
                               continue;
                            }
                            api.name = methodDoc.commentText().trim();
                            if (api.name.isEmpty()) {
                                api.name = methodDoc.name();
                            }
                            Tag[] apiNotes = methodDoc.tags("apiNote");
                            if (null != apiNotes && apiNotes.length > 0) {
                                api.name = apiNotes[0].text();
                            }
                            api.description = methodDoc.commentText().trim();
                            Tag[] authorTags = methodDoc.tags("author");
                            if (null != authorTags && authorTags.length > 0) {
                                api.author = authorTags[0].text();
                            }
                            Tag[] sinceTags = methodDoc.tags("since");
                            if (null != sinceTags && sinceTags.length > 0) {
                                api.since = sinceTags[0].text();
                            }
                            //获取参数信息
                            ParamTag[] paramTags = methodDoc.paramTags();
                            for (APIParameter apiParameter : api.apiParameters) {
                                for (ParamTag paramTag : paramTags) {
                                    if (apiParameter.name.equals(paramTag.parameterName())) {
                                        apiParameter.name = paramTag.parameterName();
                                        apiParameter.description = paramTag.parameterComment();
                                        break;
                                    }
                                }
                            }
                            //获取抛出异常信息
                            ThrowsTag[] throwsTags = methodDoc.throwsTags();
                            if (null != throwsTags && throwsTags.length > 0) {
                                api.apiExceptions = new APIException[throwsTags.length];
                                for (int i = 0; i < throwsTags.length; i++) {
                                    APIException apiException = new APIException();
                                    apiException.className = throwsTags[i].exceptionName();
                                    apiException.description = throwsTags[i].exceptionComment();
                                    api.apiExceptions[i] = apiException;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        return apiControllerList;
    }

    protected void handleReturnValue(Method method, API api) {
        api.returnValue = method.getGenericReturnType().getTypeName();
        if(method.getReturnType().isPrimitive()){
            return;
        }
        Set<String> apiEntitySet = getRecycleEntity(api.returnValue);
        //处理泛型
        {
            Type genericReturnType = method.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType) {
                Type[] types = ((ParameterizedType) genericReturnType).getActualTypeArguments();
                for (Type type : types) {
                    apiEntitySet.addAll(getRecycleEntity(type.getTypeName()));
                }
            }
        }
        api.returnEntityNameList = apiEntitySet.toArray(new String[0]);
    }

    protected Set<String> getRecycleEntity(String className) {
        Set<String> apiEntitySet = new LinkedHashSet<>();
        if(FieldUtil.needIgnoreClass(className)){
            return apiEntitySet;
        }
        Stack<String> apiEntityStack = new Stack<>();
        apiEntityStack.push(className);
        while (!apiEntityStack.isEmpty()) {
            Class clazz = null;
            try {
                clazz = ClassLoader.getSystemClassLoader().loadClass(apiEntityStack.pop());
            } catch (ClassNotFoundException e) {
                logger.warn("[加载类不存在]类名:{}", className);
                continue;
            }
            apiEntitySet.add(clazz.getName());
            if (apiEntityMap.containsKey(clazz.getName())) {
                continue;
            }
            APIEntity apiEntity = addAPIEntity(clazz);
            apiEntityMap.put(clazz.getName(), apiEntity);
            for (APIField apiField : apiEntity.apiFields) {
                if (apiField.className.startsWith("[L")) {
                    apiField.className = apiField.className.substring(2, apiField.className.length() - 1);
                } else if (apiField.className.contains("<") && apiField.className.contains(">")) {
                    apiField.className = apiField.className.substring(apiField.className.indexOf("<") + 1, apiField.className.indexOf(">"));
                }

                if(FieldUtil.needIgnoreClass(apiField.className)){
                    continue;
                }
                if (!apiEntityMap.containsKey(apiField.className)) {
                    apiEntityStack.push(apiField.className);
                }
            }
        }
        return apiEntitySet;
    }

    private APIEntity addAPIEntity(Class clazz) {
        APIEntity apiEntity = new APIEntity();
        //处理类名
        {
            apiEntity.className = clazz.getName();
            apiEntity.simpleName = clazz.getSimpleName();
        }
        //处理Field
        {
            Field[] fields = FieldUtil.getAllField(clazz);
            APIField[] apiFields = new APIField[fields.length];
            Field.setAccessible(fields, true);
            for (int i = 0; i < fields.length; i++) {
                APIField apiField = new APIField();
                apiField.name = fields[i].getName();
                apiField.className = fields[i].getType().getName();
                //处理泛型
                Type type = fields[i].getGenericType();
                if (type instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType) type;
                    Type genericType = pType.getActualTypeArguments()[0];
                    apiField.className += "<" + genericType.getTypeName() + ">";
                }
                apiFields[i] = apiField;
            }
            apiEntity.apiFields = apiFields;
        }
        try {
            apiEntity.instance = JSON.toJSONString(clazz.newInstance(), SerializerFeature.WriteMapNullValue);
        } catch (Exception e) {
            logger.warn("[实例化失败]原因:{},类名:{}",e.getMessage(),clazz.getName());
        }
        return apiEntity;
    }
}
