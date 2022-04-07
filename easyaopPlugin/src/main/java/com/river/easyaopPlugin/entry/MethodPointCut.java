package com.river.easyaopPlugin.entry;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.InsnList;

import java.util.ArrayList;

/**
 * @Author: River
 * @Emial: 1632958163@qq.com
 * @Create: 2022/4/1
 * @Desc: 注解核心数据收集类
 **/
public class MethodPointCut {
    //切面类全限定名
    private String clzName;
    //切点方法名及描述，该描述于JVM标准描述有些差异
    private String methodDesc;
    //前置炽入操作码及操作数
    private InsnList beforeInstructions;
    //后置炽入操作码及操作数
    private InsnList afterInstructions;
    //注解类
    private String annoClass;
    //lambda收集集合
    private ArrayList<Handle> lambdas = new ArrayList<>();

    public String getClzName() {
        return clzName;
    }

    public void setClzName(String clzName) {
        this.clzName = clzName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public InsnList getBeforeInstructions() {
        return beforeInstructions;
    }

    public void setBeforeInstructions(InsnList beforeInstructions) {
        this.beforeInstructions = beforeInstructions;
    }

    public InsnList getAfterInstructions() {
        return afterInstructions;
    }

    public void setAfterInstructions(InsnList afterInstructions) {
        this.afterInstructions = afterInstructions;
    }

    public String getAnnoClass() {
        return annoClass;
    }

    public void setAnnoClass(String annoClass) {
        this.annoClass = annoClass;
    }

    public ArrayList<Handle> getLambdas() {
        return lambdas;
    }

    public void setLambdas(ArrayList<Handle> lambdas) {
        this.lambdas = lambdas;
    }
}
