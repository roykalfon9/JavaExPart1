package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;

public class JumpNotZeroInstruction extends AbstractInstruction {

    public JumpNotZeroInstruction(Variable variable, Label label) {
        super(InstructionData.JUMP_NO_ZERO, variable, label);
    }

    @Override
    public void execute() {

    }
}
