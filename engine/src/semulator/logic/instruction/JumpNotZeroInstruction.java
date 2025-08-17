package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;

public class JumpNotZeroInstruction extends AbstractInstruction {

    private Label jumpTargetIfTrue;

    public JumpNotZeroInstruction(Variable variable, Label jumpTargetIfTrue) {
        super(InstructionData.JUMP_NO_ZERO, variable);
        this.jumpTargetIfTrue = jumpTargetIfTrue;
    }

    public JumpNotZeroInstruction(Variable variable, Label label, Label jumpTargetIfTrue) {
        super(InstructionData.JUMP_NO_ZERO, variable, label);
        this.jumpTargetIfTrue = jumpTargetIfTrue;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return (context.getVariablevalue(this.getVariable()) != 0) ? jumpTargetIfTrue : FixedLabel.EMPTY;
    }
}
