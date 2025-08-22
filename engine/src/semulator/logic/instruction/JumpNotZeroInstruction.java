package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
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

    public JumpNotZeroInstruction(Variable variable, Label jumpTargetIfTrue, Label label) {
        super(InstructionData.JUMP_NO_ZERO, variable, label);
        this.jumpTargetIfTrue = jumpTargetIfTrue;
    }

    public JumpNotZeroInstruction(Variable variable, Label jumpTargetIfTrue, Sinstruction parentInstruction, Label label) {
        super(InstructionData.JUMP_NO_ZERO, variable, parentInstruction, label);
        this.jumpTargetIfTrue = jumpTargetIfTrue;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return (context.getVariablevalue(this.getVariable()) != 0) ? jumpTargetIfTrue : FixedLabel.EMPTY;
    }

    @Override
    public String toDisplayString()
    {
        return String.format("#%d (%s) [ %-5s ] IF %s != 0 GOTO %s (%d)",
                this.getInstructionNumber(),
                this.isBasic(),
                this.getLabel().getLabelRepresentation(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.jumpTargetIfTrue.getLabelRepresentation(),
                this.cycles());
    }

}
