package com.river.easyaopPlugin.implantCode;

import com.river.easyaopPlugin.entry.MethodPointCut;
import com.river.easyaopPlugin.util.Util;

import org.objectweb.asm.*;

import java.io.IOException;
import java.util.List;

/**
 * @Author: River
 * @Emial: 1632958163@qq.com
 * @Create: 2022/4/1
 * @Desc: 炽入代码核心类，用于将收集到的MethodPointCut在合适的时机将维护的beforeInstructions,afterInstructions炽入到方法体内
 **/
public class ImplantCodeClassVisitor extends ClassVisitor {
    private List<MethodPointCut> methodPointCuts;
    private String clzName;

    public ImplantCodeClassVisitor(ClassVisitor classVisitor, List<MethodPointCut> methodPointCuts) {
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
        List<MethodPointCut> methodPointCuts = Util.matcherMethodPointCuts(this.methodPointCuts, clzName, name, descriptor);

        if (mv != null && !methodPointCuts.isEmpty()) {
            mv = new ImplantCodeMethodVisitor(mv, access, name, descriptor, methodPointCuts);
        }
        return mv;
    }

    public static byte[] implantCode(String clzName, List<MethodPointCut> methodPointCuts) throws IOException {
        ClassReader classReader = new ClassReader(clzName);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ImplantCodeClassVisitor implantCodeClassVisitor = new ImplantCodeClassVisitor(classWriter, methodPointCuts);
        classReader.accept(implantCodeClassVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return classWriter.toByteArray();
    }

    public static byte[] implantCode(byte[] bytes, List<MethodPointCut> methodPointCuts) {
        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ImplantCodeClassVisitor implantCodeClassVisitor = new ImplantCodeClassVisitor(classWriter, methodPointCuts);
        classReader.accept(implantCodeClassVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return classWriter.toByteArray();
    }
}
