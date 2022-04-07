package com.river.easyaopPlugin.collectAnno;

import com.river.easyaopPlugin.util.ClassTypes;
import com.river.easyaopPlugin.entry.MethodPointCut;

import org.objectweb.asm.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: River
 * @Emial: 1632958163@qq.com
 * @Create: 2022/4/1
 * @Desc: 用于收集@Aop,@AfterReturning,@AfterThrowing,@Around,@Before注解，收集注解相关的方法、操作码、操作数，收集结果存储于methodPointCuts字段中
 * MethodPointCut是该库的核心数据，为之后炽入代码提供核心逻辑支持
 **/
public class CollectAnnoClassVisitor extends ClassVisitor {
    private String clzName;
    private boolean needReduce;
    private ArrayList<MethodPointCut> methodPointCuts = new ArrayList<>();

    public CollectAnnoClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM6, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        clzName = name;
    }


    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        needReduce = descriptor.equals(ClassTypes.AopDesc);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (needReduce) {
            mv = new InstructionsCollectMethodNode(this, access, name, descriptor, signature, exceptions);
        }
        return mv;
    }

    public static List<MethodPointCut> collect(String clzName) throws IOException {
        ClassReader cr = new ClassReader(clzName);
        CollectAnnoClassVisitor cv = new CollectAnnoClassVisitor(null);
        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cv.getMethodPointCuts();
    }

    public static List<MethodPointCut> collect(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        CollectAnnoClassVisitor cv = new CollectAnnoClassVisitor(null);
        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cv.getMethodPointCuts();
    }


    public ArrayList<MethodPointCut> getMethodPointCuts() {
        return methodPointCuts;
    }

    public void setMethodPointCuts(ArrayList<MethodPointCut> methodPointCuts) {
        this.methodPointCuts = methodPointCuts;
    }
}
