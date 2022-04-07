package com.river.easyaopPlugin.util;

import com.river.easyaopPlugin.entry.MethodPointCut;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author: River
 * @Emial: 1632958163@qq.com
 * @Create: 2022/4/1
 **/
public class Util {
    //匹配基础数据类型,这里使用的类型描述符为JVM规范，唯一不同的是额外新增*表示通配
    public final static String REG_BASE_TYPE = "\\*|L[\\w/]+;|Z|C|B|S|I|F|J|D";
    //匹配基础数据类型 + 数组
    public final static String REG_BASE_WITH_ARR_TYPE = String.format("%s|(\\[+%s)", REG_BASE_TYPE, REG_BASE_TYPE);
    //匹配参数
    public final static String REG_MATCHER_ARGS = String.format("(\\*{2}$|%s)", REG_BASE_WITH_ARR_TYPE);
    //匹配方法描述
    public final static String REG_MATCHER_DESC = String.format("([a-zA-Z][a-zA-Z0-9\\$]+|\\*)\\((%s)*\\)", REG_BASE_WITH_ARR_TYPE);

    /**
     * 判断描述是否匹配注解
     * @param descriptor
     * @return
     */
    public static boolean matcherAnno(String descriptor) {
        return descriptor.equals(ClassTypes.AfterDesc) ||
                descriptor.equals(ClassTypes.AfterReturningDesc) ||
                descriptor.equals(ClassTypes.AfterThrowingDesc) ||
                descriptor.equals(ClassTypes.AroundDesc) ||
                descriptor.equals(ClassTypes.BeforeDesc);
    }

    /**
     * 根据方法描述获取参数类型集合
     * @param methodDescriptor
     * @return
     */
    public static List<String> getArgumentTypes(String methodDescriptor) {
        ArrayList<String> argsTypes = new ArrayList<>();
        if (!methodDescriptor.matches(REG_MATCHER_DESC)) {
            return null;
        }
        String args = methodDescriptor.substring(methodDescriptor.indexOf('(') + 1, methodDescriptor.length() - 1);
        Pattern pattern = Pattern.compile(REG_MATCHER_ARGS);
        Matcher matcher = pattern.matcher(args);
        while (matcher.find()) {
            argsTypes.add(matcher.group(0));
        }
        return argsTypes;
    }

    /**
     * 根据方法描述获取方法名
     * @param methodDescriptor
     * @return
     */
    public static String getMethodName(String methodDescriptor) {
        return methodDescriptor.substring(0, methodDescriptor.indexOf('('));
    }

    /**
     * 根据MethodVisitor.visitMethod相关参数匹配MethodPointCut集合
     * @param methodPointCuts
     * @param clzName
     * @param methodName
     * @param methodDescriptor
     * @return
     */
    public static List<MethodPointCut> matcherMethodPointCuts(List<MethodPointCut> methodPointCuts, String clzName, String methodName, String methodDescriptor) {
        return methodPointCuts.stream().filter(methodPointCut -> {
            boolean matcher = matcherMethodDesc(methodPointCut, clzName, methodName, methodDescriptor);
            matcher = methodPointCut.getLambdas().stream().anyMatch((handle) -> handle.getOwner().equals(clzName) && handle.getName().equals(methodName) && handle.getDesc().equals(methodDescriptor)) || matcher;
            return matcher;
        }).collect(Collectors.toList());
    }

    /**
     * 根据MethodVisitor.visitMethod相关参数匹配MethodPointCut
     * @param methodPointCut
     * @param clzName
     * @param methodName
     * @param methodDescriptor
     * @return
     */
    public static boolean matcherMethodDesc(MethodPointCut methodPointCut, String clzName, String methodName, String methodDescriptor) {
        boolean matcher = methodPointCut.getClzName().equals(clzName) || methodPointCut.getClzName().equals("*");
        if (matcher) {
            String targetMethodName = getMethodName(methodPointCut.getMethodDesc());
            List<String> targetArgs = getArgumentTypes(methodPointCut.getMethodDesc());

            matcher = targetMethodName.equals(methodName) || targetMethodName.equals("*");

            if (matcher) {
                Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);

                if (targetArgs == null || targetArgs.isEmpty() && argumentTypes.length > 0) {
                    matcher = false;
                } else {
                    for (String targetArg : targetArgs) {
                        if (targetArg.equals("**")) {
                            matcher = true;
                            break;
                        }

                        int index = targetArgs.indexOf(targetArg);

                        if (index < argumentTypes.length) {
                            Type arg = argumentTypes[index];

                            if (!(arg.getDescriptor().equals(targetArg) || targetArg.equals("*"))) {
                                matcher = false;
                                break;
                            }
                        } else {
                            matcher = false;
                            break;
                        }
                    }
                }
            }
        }
        return matcher;
    }

    /**
     * 根据描述返回相应的注解类
     * @param descriptor
     * @return
     */
    public static String matcherAnnoClass(String descriptor) {
        if (descriptor.equals(ClassTypes.AfterDesc)) {
            return ClassTypes.After;
        }
        if (descriptor.equals(ClassTypes.AfterReturningDesc)) {
            return ClassTypes.AfterReturning;
        }
        if (descriptor.equals(ClassTypes.AfterThrowingDesc)) {
            return ClassTypes.AfterThrowing;
        }
        if (descriptor.equals(ClassTypes.AroundDesc)) {
            return ClassTypes.Around;
        }
        if (descriptor.equals(ClassTypes.BeforeDesc)) {
            return ClassTypes.Before;
        }
        return null;
    }
}
