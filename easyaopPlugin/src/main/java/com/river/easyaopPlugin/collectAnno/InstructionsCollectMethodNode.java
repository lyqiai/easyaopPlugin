package com.river.easyaopPlugin.collectAnno;


import com.river.easyaopPlugin.util.ClassTypes;
import com.river.easyaopPlugin.entry.MethodPointCut;
import com.river.easyaopPlugin.util.Util;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.InvocationTargetException;

/**
 * @Author: River
 * @Emial: 1632958163@qq.com
 * @Create: 2022/4/1
 * @Desc: 该类通过实现MethodNode实现收集相关操作码和操作数
 * 环绕炽入代码需要特定的逻辑处理，核心逻辑通过劫持#Method.invoke实现炽入代码的分层逻辑
 **/
public class InstructionsCollectMethodNode extends MethodNode {
    private CollectAnnoClassVisitor cv;
    private boolean isMatchAnno = false;
    private MethodPointCut methodPointCut = new MethodPointCut();

    public InstructionsCollectMethodNode(CollectAnnoClassVisitor cv, int access, String name, String descriptor, String signature, String[] exceptions) {
        super(Opcodes.ASM6, access, name, descriptor, signature, exceptions);
        this.cv = cv;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        isMatchAnno = Util.matcherAnno(descriptor);

        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);

        if (isMatchAnno) {
            methodPointCut.setAnnoClass(Util.matcherAnnoClass(descriptor));
            av = new AnnotationVisitor(Opcodes.ASM6, av) {
                @Override
                public void visit(String name, Object value) {
                    super.visit(name, value);
                    try {
                        name = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
                        java.lang.reflect.Method setField = methodPointCut.getClass().getDeclaredMethod(name, String.class);
                        setField.invoke(methodPointCut, value);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        return av;
    }

    @Override
    public void visitMethodInsn(int opcodeAndSource, String owner, String name, String descriptor, boolean isInterface) {
        if (isMatchAnno && opcodeAndSource == Opcodes.INVOKEINTERFACE && owner.equals(ClassTypes.Method) && name.equals("invoke") && descriptor.equals("()V") && isInterface) {
            instructions.remove(instructions.getLast());
            InsnList insnList = new InsnList();
            insnList.add(instructions);
            methodPointCut.setBeforeInstructions(insnList);
            instructions.clear();
            return;
        }
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
    }


    @Override
    public void visitEnd() {
        super.visitEnd();
        if (isMatchAnno) {
            InsnList insnList = new InsnList();
            instructions.remove(instructions.getLast());
            insnList.add(instructions);

            if (methodPointCut.getAnnoClass().equals(ClassTypes.Before)) {
                methodPointCut.setBeforeInstructions(insnList);
            } else {
                methodPointCut.setAfterInstructions(insnList);
            }

            cv.getMethodPointCuts().add(methodPointCut);
        }
    }
}
