package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.expansion.ExpansionIdAllocator;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;

public class IncreaseInstruction extends  AbstractInstruction {

    public IncreaseInstruction(Variable variable)
    {
        super(InstructionData.INCREASE, variable);
    }
    public IncreaseInstruction(Variable variable, Label label)
    {
        super(InstructionData.INCREASE, variable, label);
    }
    public IncreaseInstruction(Variable variable,Sinstruction parentInstruction )
    {
        super(InstructionData.INCREASE, variable,parentInstruction);
    }
    public IncreaseInstruction(Variable variable,Sinstruction parentInstruction, Label label )
    {
        super(InstructionData.INCREASE, variable,parentInstruction, label);
    }



    @Override
    public Label execute(ExecutionContext context) {
        Long varValue = context.getVariablevalue(this.getVariable());
        varValue++;
        context.updateVariable(this.getVariable(), varValue);
        return FixedLabel.EMPTY;
    }

    @Override
    public void InitializeIProgramInstruction(ExpansionIdAllocator ex)    {
    }

    @Override
    public String toDisplayString()
    {
        return String.format("#%d (%s) [%-5s] %s <- %s + 1 (%d)",
                this.getInstructionNumber(),
                this.isBasic(),
                this.getLabel().getLabelRepresentation(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.cycles());
    }

}
