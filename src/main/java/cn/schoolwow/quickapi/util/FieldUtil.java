package cn.schoolwow.quickapi.util;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class FieldUtil {
    /**
     * 是否需要忽略该类
     * @param className 类名
     * */
    public static boolean needIgnoreClass(String className){
        for(String ignorePackageName:QuickAPIConfig.ignorePackageNameList){
            if(className.startsWith(ignorePackageName)){
                return true;
            }
        }
        return false;
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
}
