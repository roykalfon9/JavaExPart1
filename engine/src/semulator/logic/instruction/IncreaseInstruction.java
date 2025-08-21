package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;

public class IncreaseInstruction extends  AbstractInstruction {

    public IncreaseInstruction(Variable variable) {
        super(InstructionData.INCREASE, variable);
    }
    public IncreaseInstruction(Variable variable, Label label) {
        super(InstructionData.INCREASE, variable, label);
    }

    @Override
    public Label execute(ExecutionContext context) {
        Long varValue = context.getVariablevalue(this.getVariable());
        varValue++;
        context.updateVariable(this.getVariable(), varValue);
        return FixedLabel.EMPTY;
    }
}
