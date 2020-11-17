package cn.schoolwow.quickapi.domain;

import cn.schoolwow.quickapi.util.QuickAPIConfig;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;

/**API微服务*/
public class APIMicroService implements APIMicro {
    private static Logger logger = LoggerFactory.getLogger(APIMicroService.class);
    /**路径前缀*/
    public String prefix;
    /**控制器包名*/
    public Set<String> controllerPackageNameList = new LinkedHashSet<>();
    /**控制器类*/
    public List<String> controllerClassNameList = new ArrayList<>();
    /**要忽略的类*/
    public List<String> ignoreClassList = new ArrayList<>();
    /**要忽略的包名*/
    public List<String> ignorePackageNameList = new ArrayList<>(Arrays.asList(
            "java.",
            "javax.",
            "org.springframework",
            "cn.schoolwow.quickserver"
    ));
    /**函数式接口过滤类*/
    public Predicate<String> predicate;

    /**
     * 扫描controller层
     * @param packageName 扫描Controller包
     * */
    public APIMicroService controller(String packageName){
        controllerPackageNameList.add(packageName);
        return this;
    }

    /**
     * 扫描controller层
     * @param className 扫描单个Controller类
     * */
    public APIMicroService controllerClass(String className){
        controllerClassNameList.add(className);
        return this;
    }

    /**
     * 接口路径前缀
     * @param prefix 接口路径前缀(context-path)
     * */
    public APIMicroService prefix(String prefix){
        if(null==prefix||prefix.isEmpty()){
            return this;
        }
        if(!prefix.startsWith("/")){
            prefix = "/"+prefix;
        }
        this.prefix = prefix;
        return this;
    }

    /**
     * Java源代码路径
     * @param sourcePath 指定java源代码所在目录
     * */
    public APIMicroService sourcePath(String sourcePath){
        //判断文件目录是否存在
        if(Files.notExists(Paths.get(sourcePath))){
            logger.warn("[源文件路径不存在]路径:{}",sourcePath);
            return this;
        }
        QuickAPIConfig.sourcePathBuilder.append(sourcePath+";");
        return this;
    }

    /**
     * Java类路径
     * @param classPathURL 指定java类文件
     * */
    public APIMicroService classPath(URL classPathURL){
        QuickAPIConfig.classPathList.add(classPathURL);
        return this;
    }

    /**
     * 扫描pom.xml获取相关依赖
     * @param pomFilePath pom.xml路径
     * */
    public APIMicroService pom(String pomFilePath) {
        //查看Maven环境变量是否设置
        String mavenHome = System.getenv("MAVEN_HOME");
        if(StringUtils.isEmpty(mavenHome)){
            throw new IllegalArgumentException("请先设置MAVEN_HOME环境变量");
        }
        //检查pom文件是否存在
        File pomFile = new File(pomFilePath);
        if(!pomFile.exists()){
            throw new IllegalArgumentException("pom.xml文件不存在!路径:"+pomFilePath);
        }
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomFile);
        request.setGoals(Arrays.asList("dependency:list"));
        Properties properties = new Properties();
        properties.setProperty("outputFile", System.getProperty("user.dir")+"/dependencies.txt");
        properties.setProperty("outputAbsoluteArtifactFilename", "true");
        properties.setProperty("includeScope", "runtime");
        request.setProperties(properties);

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(mavenHome));
        invoker.setOutputHandler(null);
        try{
            InvocationResult result = invoker.execute(request);
            if (0!=result.getExitCode()) {
                throw new IllegalStateException("Maven执行失败!"+result.getExecutionException().getMessage());
            }
            Path path = Paths.get(System.getProperty("user.dir")+"/dependencies.txt");
            List<String> lines = Files.readAllLines(path);
            for(int i=1;i<lines.size();i++){
                String jarPath = lines.get(i).substring(lines.get(i).lastIndexOf(":")+1);
                classPath(new File(jarPath).toURL());
            }
        }catch (IOException |MavenInvocationException e){
            e.printStackTrace();
        }
        return this;
    }

    /**
     * 指定类库位置
     * @param libDirectory lib库位置
     * */
    public APIMicroService lib(String libDirectory) {
        Path path = Paths.get(libDirectory);
        if(Files.exists(path)&&Files.isDirectory(path)){
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if(file.toFile().getName().endsWith(".jar")){
                            classPath(file.toUri().toURL());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("[读取lib目录异常]{}",e.getMessage());
            }
        }else{
            logger.warn("[lib路径不存在]{}",libDirectory);
        }
        return this;
    }

    /**
     * 忽略包名
     * @param ignorePackageName 要忽略的包名
     * */
    public APIMicroService ignorePackageName(String ignorePackageName){
        ignorePackageNameList.add(ignorePackageName);
        return this;
    }

    /**
     * 忽略类
     * @param ignoreClassName 要忽略的类名
     * */
    public APIMicroService ignoreClass(String ignoreClassName){
        ignoreClassList.add(ignoreClassName);
        return this;
    }

    /**
     * 扫描类过滤接口
     * @param predicate 函数式接口 参数为类名
     * */
    public APIMicroService filter(Predicate<String> predicate){
        this.predicate = predicate;
        return this;
    }
}
