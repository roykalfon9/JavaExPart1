package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.expansion.ExpansionIdAllocator;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;

public class DecreaseInstruction extends AbstractInstruction{

    public DecreaseInstruction(Variable variable)
    {
        super(InstructionData.DECREASE, variable);
    }
    public DecreaseInstruction(Variable variable, Label label)
    {
        super(InstructionData.DECREASE, variable, label);
    }
    public DecreaseInstruction(Variable variable, Sinstruction parentInstruction)
    {
        super(InstructionData.DECREASE, variable,parentInstruction);
    }
    public DecreaseInstruction(Variable variable, Sinstruction parentInstruction, Label label)
    {
        super(InstructionData.DECREASE, variable,parentInstruction, label);
    }

    @Override
    public Label execute(ExecutionContext context) {
        Long varValue = context.getVariablevalue(this.getVariable());
        if (varValue > 0)
        {
            context.updateVariable(this.getVariable(), varValue - 1);
        }
        return FixedLabel.EMPTY;
    }

    @Override
    public void InitializeIProgramInstruction (ExpansionIdAllocator ex)
    {
    }


    @Override
     public String toDisplayString()
    {
        return String.format("#%d (%s) [%-5s] %s <- %s - 1 (%d)",
                this.getInstructionNumber(),
                this.isBasic(),
                this.getLabel().getLabelRepresentation(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.cycles());
    }

}
