package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.expansion.ExpansionIdAllocator;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.label.LabelImp;
import semulator.logic.program.SprogramImpl;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;

public class ConstantAssignmentInstruction extends AbstractInstruction{

    public ConstantAssignmentInstruction(Variable variable, long constValue)
    {
        super(InstructionData.CONSTANT_ASSIGNMENT, variable);
        this.constValue = constValue;
    }
    public ConstantAssignmentInstruction(Variable variable, long constValue, Label label)
    {
        super(InstructionData.CONSTANT_ASSIGNMENT, variable, label);
        this.constValue = constValue;
    }
    public ConstantAssignmentInstruction(Variable variable, long constValue, Sinstruction parentInstruction)
    {
        super(InstructionData.CONSTANT_ASSIGNMENT, variable, parentInstruction);
        this.constValue = constValue;
    }
    public ConstantAssignmentInstruction(Variable variable, long constValue, Sinstruction parentInstruction, Label label )
    {
        super(InstructionData.CONSTANT_ASSIGNMENT, variable, parentInstruction, label);
        this.constValue = constValue;
    }

    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(this.getVariable(), constValue);
        return FixedLabel.EMPTY;
    }

    @Override
    public void InitializeIProgramInstruction (ExpansionIdAllocator ex){
        this.instructionProgram = new SprogramImpl("expand");

        Label label;
        if (this.getLabel() != FixedLabel.EMPTY)
        {
            label = this.getLabel();
        }
        else
        {
            label = new LabelImp(ex.getLabelNumber());
        }

        ZeroVariableInstruction zeroVariable = new ZeroVariableInstruction(this.getVariable(),this, label);

        this.getInstructionProgram().addInstruction(zeroVariable);
        for (long i=0; i<this.getConstValue();i++)
        {
            this.getInstructionProgram().addInstruction(new IncreaseInstruction(this.getVariable(), this));
        }
    }

    @Override
    public String toDisplayString()
    {
        return String.format("[%-5s]  %s <- %d",
                this.getLabel().getLabelRepresentation(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.getConstValue()
                );
    }

}
