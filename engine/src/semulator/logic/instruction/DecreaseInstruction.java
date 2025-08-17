package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;

public class DecreaseInstruction extends AbstractInstruction{

    public DecreaseInstruction(Variable variable) {
        super(InstructionData.DECREASE, variable);
    }
    public DecreaseInstruction(Variable variable, Label label) {
        super(InstructionData.DECREASE, variable, label);
    }

    @Override
    public Label execute(ExecutionContext context) {
        Long varValue = context.getVariablevalue(this.getVariable());
        context.updateVariable(this.getVariable(), varValue - 1);
        return FixedLabel.EMPTY;
    }
}
