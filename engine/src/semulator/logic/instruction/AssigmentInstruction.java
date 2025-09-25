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

public class AssigmentInstruction extends AbstractInstruction{

    public AssigmentInstruction(Variable variable, Variable secondaryVariable)
    {
        super(InstructionData.ASSIGNMENT, variable);
        this.secondaryVariable = secondaryVariable;
    }
    public AssigmentInstruction(Variable variable, Variable secondaryVariable, Label label)
    {
        super(InstructionData.ASSIGNMENT, variable, label);
        this.secondaryVariable = secondaryVariable;
    }
    public AssigmentInstruction(Variable variable, Variable secondaryVariable, Sinstruction parentInstruction)
    {
        super(InstructionData.ASSIGNMENT, variable, parentInstruction);
         this.secondaryVariable = secondaryVariable;
    }
    public AssigmentInstruction(Variable variable, Variable secondaryVariable, Sinstruction parentInstruction, Label label )
    {
        super(InstructionData.ASSIGNMENT, variable, parentInstruction, label);
        this.secondaryVariable = secondaryVariable;
    }

    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(this.getVariable(), context.getVariablevalue(secondaryVariable));
        return FixedLabel.EMPTY;
    }

    @Override
    public void InitializeIProgramInstruction (ExpansionIdAllocator ex)    {
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

        Label l1 = new LabelImp(ex.getLabelNumber());
        Label l2 = new LabelImp(ex.getLabelNumber());
        Label l3 = new LabelImp(ex.getLabelNumber());

        Variable z1 =  new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber());

        ZeroVariableInstruction zeroVariable = new ZeroVariableInstruction(this.getVariable(),this, label);
        JumpNotZeroInstruction jumpNotZero = new JumpNotZeroInstruction(this.secondaryVariable,l1,this);
        GoToLabelInstruction joToLabel = new GoToLabelInstruction(this.getVariable(),l3,this);
        DecreaseInstruction decrease = new DecreaseInstruction(this.secondaryVariable,this,l1);
        IncreaseInstruction increment = new IncreaseInstruction(z1,this);
        JumpNotZeroInstruction jumpNotZero2 = new JumpNotZeroInstruction(this.secondaryVariable,l1,this);
        DecreaseInstruction decrease2 = new DecreaseInstruction(z1,this,l2);
        IncreaseInstruction  increase2 = new IncreaseInstruction(this.getVariable(),this);
        IncreaseInstruction  increase3 = new IncreaseInstruction(this.secondaryVariable,this);
        JumpNotZeroInstruction jumpNotZero3 = new JumpNotZeroInstruction(z1,l2,this);
        NeutralInstruction neutral = new NeutralInstruction(this.getVariable(),this,l3);

        this.getInstructionProgram().addInstruction(zeroVariable);
        this.getInstructionProgram().addInstruction(jumpNotZero);
        this.getInstructionProgram().addInstruction(joToLabel);
        this.getInstructionProgram().addInstruction(decrease);
        this.getInstructionProgram().addInstruction(increment);
        this.getInstructionProgram().addInstruction(jumpNotZero2);
        this.getInstructionProgram().addInstruction(decrease2);
        this.getInstructionProgram().addInstruction(increase2);
        this.getInstructionProgram().addInstruction(increase3);
        this.getInstructionProgram().addInstruction(jumpNotZero3);
        this.getInstructionProgram().addInstruction(neutral);

    }

    @Override
    public String toDisplayString()
    {
        return String.format("[%-5s] %s <- %s",
                this.getLabel().getLabelRepresentation(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.secondaryVariable.getRepresentation().toLowerCase());
    }
}
