package com.tiefan.framework.conflict.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author gavinwu
 * @date 2018/5/31 14:37
 */

/**
 * 扫描jar中class类冲突（相同包命和类名的即为冲突文件）
 */
@Mojo(name = "check",defaultPhase = LifecyclePhase.PACKAGE)
public class ConflictCheckMojo extends AbstractMojo {

    /**
     * class类文件容器
     */
    private HashMap<String,String> classFind = new HashMap<String,String>();

    @Parameter(defaultValue = "${basedir}")
    private String rootPath;
    @Parameter(property = "exceptions")
    private String[] exceptions;
    @Parameter(property = "excludes")
    private String[] excludes;

    public String[] getExceptions() {
        return exceptions;
    }

    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions;
    }

    public String[] getExcludes() {
        return excludes;
    }

    public void setExcludes(String[] excludes) {
        this.excludes = excludes;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Override
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("conflict check begin...");
            if (rootPath == null || rootPath.equals("")) {
                throw new MojoExecutionException("rootPath must be not null");
            }
            getLog().info("root path : " + rootPath);
            File rootFile = new File(rootPath);
            if (!rootFile.isDirectory()) {
                throw new MojoExecutionException("rootPath is invalid");
            }
            String[] dirs = rootFile.list();
            for (int i = 0; i < dirs.length; i++) {
                if (dirs[i].equals("target")) {
                    check(rootPath + File.separator + dirs[i]);
                    break;
                }
            }
            getLog().info("check number:"+classFind.size());
            getLog().info("conflict check end...");
        }finally {
            classFind.clear();
        }
    }

    /**
     * 根据根目录逐级搜索并检查
     * @param filePath
     * @throws MojoExecutionException
     */
    public void check(String filePath) throws MojoExecutionException {
        File file = null;
        if(filePath==null || filePath.equals("")){
            return ;
        }
        file = new File(filePath);
        if(file.isDirectory()){
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                check(filePath+File.separator+files[i]);
            }
        }else{
            if(file.getName().endsWith(".jar") && file.getAbsolutePath().contains("WEB-INF\\lib")) {
                try {
                    JarFile jarFile = new JarFile(file);
                    Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
                    while(jarEntryEnumeration.hasMoreElements()){
                        JarEntry jarEntry = jarEntryEnumeration.nextElement();
                        String fileOfPackagePath = jarEntry.getName();
                        if(fileOfPackagePath.endsWith(".class")) {
                            fileOfPackagePath = fileOfPackagePath.replaceAll("/", ".");
                            if (classFind.get(fileOfPackagePath) == null) {
                                classFind.put(fileOfPackagePath, file.getName());
                            } else {
                                boolean flag = true;
                                if(exceptions!=null && exceptions.length>0){
                                    for (int i = 0; i < exceptions.length; i++) {
                                        if(exceptions[i].equals(fileOfPackagePath)){
                                            getLog().warn("framework-> class conflict " + fileOfPackagePath + " ==== 【" + file.getName() + "】<-- conflict -->【" + classFind.get(fileOfPackagePath)+"】");
                                            flag = false;continue;
                                        }
                                    }
                                }
                                if(flag) {
                                    if (excludes != null && excludes.length > 0) {
                                        for (int i = 0; i < excludes.length; i++) {
                                            if (excludes[i].equals(fileOfPackagePath)) {
                                                getLog().warn("project-> class conflict " + fileOfPackagePath + " ==== 【" + file.getName() + "】<-- conflict -->【" + classFind.get(fileOfPackagePath) + "】");
                                                flag = false;
                                                continue;
                                            }
                                        }
                                    }
                                }
                                if(flag) {
                                    throw new MojoExecutionException("class conflict " + fileOfPackagePath + " ==== 【" + file.getName() + "】<-- conflict -->【" + classFind.get(fileOfPackagePath) + "】");
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
