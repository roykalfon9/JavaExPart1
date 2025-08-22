package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
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
    public NeutralInstruction(Variable variable, Sinstruction parentInstruction, Label label) {
        super(InstructionData.NEUTRAL, variable, parentInstruction, label);
    }

    @Override
    public Label execute(ExecutionContext context) {
        return FixedLabel.EMPTY;
    }

    @Override
    public String toDisplayString()
    {
        return String.format("#%d (%s) [ %-5s ]  %s <- %s (%d)",
                this.getInstructionNumber(),
                this.isBasic(),
                this.getLabel().getLabelRepresentation(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.cycles());
    }

}
