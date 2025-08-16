package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;

public class NoOpInstruction extends AbstractInstruction {

    public NoOpInstruction(Variable variable) {
        super(InstructionData.NO_OP, variable);
    }
    public NoOpInstruction(Variable variable, Label label) {
        super(InstructionData.NO_OP, variable, label);
    }


    @Override
    public void execute() {

    }

}
