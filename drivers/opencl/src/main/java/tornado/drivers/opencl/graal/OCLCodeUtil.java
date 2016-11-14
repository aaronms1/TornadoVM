package tornado.drivers.opencl.graal;

import com.oracle.graal.compiler.common.LIRKind;
import com.oracle.graal.lir.Variable;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CallingConvention.Type;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Signature;

public class OCLCodeUtil {

    /**
     * Create a calling convention from a {@link ResolvedJavaMethod}.
     */
    public static CallingConvention getCallingConvention(CodeCacheProvider codeCache, CallingConvention.Type type, ResolvedJavaMethod method, boolean stackOnly) {
        Signature sig = method.getSignature();
        JavaType retType = sig.getReturnType(method.getDeclaringClass());
        int sigCount = sig.getParameterCount(false);
        JavaType[] argTypes;
        int argIndex = 0;
        if (!method.isStatic()) {
            argTypes = new JavaType[sigCount + 1];
            argTypes[argIndex++] = method.getDeclaringClass();
        } else {
            argTypes = new JavaType[sigCount];
        }
        for (int i = 0; i < sigCount; i++) {
            argTypes[argIndex++] = sig.getParameterType(i, null);
        }

        return getCallingConvention(type, retType, argTypes, codeCache.getTarget(), stackOnly);
    }

    private static CallingConvention getCallingConvention(Type type,
            JavaType returnType, JavaType[] argTypes, TargetDescription target,
            boolean stackOnly) {

        int variableIndex = 0;

        Variable[] inputParameters = new Variable[argTypes.length];
        for (int i = 0; i < argTypes.length; i++, variableIndex++) {
            inputParameters[i] = new Variable(LIRKind.value(target.arch.getPlatformKind(argTypes[i].getJavaKind())), variableIndex);
            //Tornado.info("arg[%d] : %s",i,inputParameters[i].getLIRKind().getPlatformKind().toString());
            //Tornado.info("type[%d]: %s",i,inputParameters[i].getKind().getDeclaringClass().getName());
        }

        JavaKind returnKind = returnType == null ? JavaKind.Void : returnType.getJavaKind();
        LIRKind lirKind = LIRKind.value(target.arch.getPlatformKind(returnKind));

        Variable returnParameter = new Variable(lirKind, variableIndex);
        variableIndex++;

        return new CallingConvention(0, returnParameter, inputParameters);
    }

}