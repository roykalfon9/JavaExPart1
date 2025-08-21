package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;

public class NeutralInstruction extends AbstractInstruction {

    public NeutralInstruction(Variable variable) {
        super(InstructionData.NEUTRAL, variable);
    }
    public NeutralInstruction(Variable variable, Label label) {
        super(InstructionData.NEUTRAL, variable, label);
    }


    @Override
    public Label execute(ExecutionContext context) {
        return FixedLabel.EMPTY;
    }

}
