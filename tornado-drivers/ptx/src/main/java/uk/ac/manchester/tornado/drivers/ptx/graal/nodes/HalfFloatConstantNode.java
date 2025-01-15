package uk.ac.manchester.tornado.drivers.ptx.graal.nodes;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;
import org.graalvm.compiler.core.common.LIRKind;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.lir.ConstantValue;
import org.graalvm.compiler.lir.Variable;
import org.graalvm.compiler.lir.gen.LIRGeneratorTool;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.ConstantNode;
import org.graalvm.compiler.nodes.calc.FloatingNode;
import org.graalvm.compiler.nodes.spi.LIRLowerable;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import uk.ac.manchester.tornado.drivers.ptx.graal.HalfFloatStamp;
import uk.ac.manchester.tornado.drivers.ptx.graal.lir.PTXKind;
import uk.ac.manchester.tornado.drivers.ptx.graal.lir.PTXLIRStmt;

@NodeInfo
public class HalfFloatConstantNode extends FloatingNode implements LIRLowerable {

    public static final NodeClass<HalfFloatConstantNode> TYPE = NodeClass.create(HalfFloatConstantNode.class);

    @Input
    private ConstantNode halfFloatValue;

    public HalfFloatConstantNode(ConstantNode halfFloatValue) {
        super(TYPE, new HalfFloatStamp());
        this.halfFloatValue = halfFloatValue;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        // the value to be written is in float format, so the bytecodes to convert
        // to half float need to be generated
        LIRGeneratorTool tool = generator.getLIRGeneratorTool();
        Value value = generator.operand(halfFloatValue);
        Variable intermediate = tool.newVariable(LIRKind.value(PTXKind.F32));
        Variable result = tool.newVariable(LIRKind.value(PTXKind.F16));
        tool.append(new PTXLIRStmt.ConvertHalfFloatStmt(result, value, intermediate));
        generator.setResult(this, result);
    }
}
