package com.river.easyaopPlugin.collectLambda;

import com.river.easyaopPlugin.entry.MethodPointCut;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.List;

/**
 * @Author: River
 * @Emial: 1632958163@qq.com
 * @Create: 2022/4/1
 * @Desc: 收集MethodPointCut相关lambda表达式的实现
 **/
public class CollectLambdaClassVisitor extends ClassVisitor {
    private List<MethodPointCut> methodPointCuts;
    private String clzName;

    public CollectLambdaClassVisitor(ClassVisitor classVisitor, List<MethodPointCut> methodPointCuts) {
        super(Opcodes.ASM6, classVisitor);
        this.methodPointCuts = methodPointCuts;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        clzName = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new CollectLambdaMethodVisitor(mv, this, methodPointCuts);
    }


    public static void collect(String clzName, List<MethodPointCut> methodPointCuts) throws IOException {
        ClassReader cr = new ClassReader(clzName);
        CollectLambdaClassVisitor cv = new CollectLambdaClassVisitor(null, methodPointCuts);
        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    public static void collect(byte[] bytes, List<MethodPointCut> methodPointCuts) {
        ClassReader cr = new ClassReader(bytes);
        CollectLambdaClassVisitor cv = new CollectLambdaClassVisitor(null, methodPointCuts);
        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    public String getClzName() {
        return clzName;
    }

    public void setClzName(String clzName) {
        this.clzName = clzName;
    }
}
