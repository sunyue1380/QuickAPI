package cn.schoolwow.quickapi.util;

import cn.schoolwow.quickapi.domain.*;
import com.sun.javadoc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static cn.schoolwow.quickapi.util.QuickAPIConfig.apiDocument;
import static cn.schoolwow.quickapi.util.QuickAPIConfig.urlClassLoader;

public class QuickAPIUtil {
    private static Logger logger = LoggerFactory.getLogger(QuickAPIUtil.class);
    /**初始化类路径*/
    public static void initClassPath(){
        QuickAPIConfig.classPathList.addAll(Arrays.asList(((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()));
        StringBuilder builder = new StringBuilder();
        urlClassLoader = new URLClassLoader(QuickAPIConfig.classPathList.toArray(new URL[0]));
        URL[] urls = urlClassLoader.getURLs();
        for(URL url:urls){
            builder.append(url.getPath().substring(1)+";");
        }
        JavaDocReader.classPath = builder.toString();
    }

    /**
     * 扫描控制器包
     * */
    public static Set<String> scanControllerPackage(){
        Set<String> classNameSet = new HashSet<>();
        for(String packageName:QuickAPIConfig.controllerPackageNameList.toArray(new String[0])){
            String packageNamePath = packageName.replace(".", "/");
            try {
                Enumeration<URL> urlEnumeration = urlClassLoader.getResources(packageNamePath);
                while(urlEnumeration.hasMoreElements()){
                    URL url = urlEnumeration.nextElement();
                    if(url==null){
                        continue;
                    }
                    logger.info("[类文件路径]包路径:{},URL:{}", packageNamePath,url.toString());
                    switch (url.getProtocol()) {
                        case "file": {
                            File file = new File(url.getFile());
                            //TODO 对于有空格或者中文路径会无法识别
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
                                        if(!shouldFilterClass(className)){
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
                                            if(!shouldFilterClass(className)){
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
        return classNameSet;
    }

    /**
     * 匹配JavaDoc注释
     * @param classNameSet 控制器类名集合
     * */
    public static void updateJavaDoc(Set<String> classNameSet){
        //匹配控制器文档
        {
            ClassDoc[] classDocs = JavaDocReader.getJavaDoc(classNameSet);
            for (APIController apiController : apiDocument.apiControllerList) {
                for (ClassDoc classDoc : classDocs) {
                    //判断控制器类名是否匹配
                    if (!apiController.className.equals(classDoc.qualifiedName())) {
                        continue;
                    }
                    apiController.setName(classDoc.commentText().trim());
                    //匹配方法
                    MethodDoc[] methodDocs = classDoc.methods();
                    for (API api : apiController.apiList) {
                        for (MethodDoc methodDoc : methodDocs) {
                            //方法名和参数类型,个数匹配才算匹配
                            if(!api.method.getName().equals(methodDoc.name())){
                                continue;
                            }
                            if(api.method.getParameterCount()!=methodDoc.parameters().length){
                                continue;
                            }
                            api.setName(methodDoc.commentText().trim());
                            api.setName(methodDoc.name());
                            Tag[] apiNotes = methodDoc.tags("apiNote");
                            if (null != apiNotes && apiNotes.length > 0) {
                                api.setName(apiNotes[0].text());
                            }
                            api.setDescription(methodDoc.commentText().trim());
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
                                    if (apiParameter.parameter.getName().equals(paramTag.parameterName())) {
                                        apiParameter.setName(paramTag.parameterName());
                                        apiParameter.setDescription(paramTag.parameterComment());
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
        //匹配实体类文档
        {
            ClassDoc[] classDocs = JavaDocReader.getJavaDoc(apiDocument.apiEntityMap.keySet());
            for (APIEntity apiEntity : apiDocument.apiEntityMap.values()) {
                for (ClassDoc classDoc : classDocs) {
                    if (apiEntity.className.equals(classDoc.qualifiedName())) {
                        apiEntity.setDescription(classDoc.commentText());
                        Tag[] authorTags = classDoc.tags("author");
                        if (null != authorTags && authorTags.length > 0) {
                            apiEntity.author = authorTags[0].text();
                        }
                        Tag[] sinceTags = classDoc.tags("since");
                        if (null != sinceTags && sinceTags.length > 0) {
                            apiEntity.since = sinceTags[0].text();
                        }
                        for (APIField apiField : apiEntity.apiFields) {
                            for (FieldDoc fieldDoc : QuickAPIUtil.getAllFieldDoc(classDoc)) {
                                if (apiField.name.equals(fieldDoc.name())) {
                                    apiField.setDescription(fieldDoc.getRawCommentText());
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * 获得该类所有字段(包括父类字段)
     * @param clazz 类
     * */
    public static Field[] getAllField(Class clazz){
        List<Field> fieldList = new ArrayList<>();
        Class tempClass = clazz;
        while (null != tempClass) {
            Field[] fields = tempClass.getDeclaredFields();
            //排除静态变量和常量
            Field.setAccessible(fields, true);
            for (Field field : fields) {
                if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                fieldList.add(field);
            }
            tempClass = tempClass.getSuperclass();
            if (null!=tempClass&&"java.lang.Object".equals(tempClass.getName())) {
                break;
            }
        }
        return fieldList.toArray(new Field[0]);
    }

    /**
     * 获得该类所有字段(包括父类字段)
     * @param classDoc 类文档
     * */
    public static FieldDoc[] getAllFieldDoc(ClassDoc classDoc){
        List<FieldDoc> fieldDocList = new ArrayList<>();
        ClassDoc tempClassDoc = classDoc;
        while (null != tempClassDoc) {
            FieldDoc[] fieldDocs = tempClassDoc.fields();
            //排除静态变量和常量
            for(FieldDoc fieldDoc:fieldDocs){
                if(Modifier.isFinal(fieldDoc.modifierSpecifier())||Modifier.isStatic(fieldDoc.modifierSpecifier())){
                    continue;
                }
                fieldDocList.add(fieldDoc);
            }
            tempClassDoc = tempClassDoc.superclass();
            if (null!=tempClassDoc&&"java.lang.Object".equals(tempClassDoc.qualifiedName())) {
                break;
            }
        }
        return fieldDocList.toArray(new FieldDoc[0]);
    }

    /**
     * 是否需要过滤该类
     * @param className 类名
     * */
    private static boolean shouldFilterClass(String className){
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
}
