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

public class JumpEqualVariableInstruction extends AbstractInstruction{

    public JumpEqualVariableInstruction(Variable variable, Label jumpTo, Variable secondaryVariable)
    {
        super(InstructionData.JUMP_EQUAL_VARIABLE, variable);
        this.secondaryVariable = secondaryVariable;
        this.jumpTo = jumpTo;
    }
    public JumpEqualVariableInstruction(Variable variable, Label jumpTo, Variable secondaryVariable, Label label)
    {
        super(InstructionData.JUMP_EQUAL_VARIABLE, variable, label);
        this.secondaryVariable = secondaryVariable;
        this.jumpTo = jumpTo;
    }
    public JumpEqualVariableInstruction(Variable variable, Label jumpTo, Variable secondaryVariable, Sinstruction parentInstruction)
    {
        super(InstructionData.JUMP_EQUAL_VARIABLE, variable, parentInstruction);
        this.secondaryVariable = secondaryVariable;
        this.jumpTo = jumpTo;
    }
    public JumpEqualVariableInstruction(Variable variable, Label jumpTo, Variable secondaryVariable, Sinstruction parentInstruction, Label label )
    {
        super(InstructionData.JUMP_EQUAL_VARIABLE, variable, parentInstruction, label);
        this.secondaryVariable = secondaryVariable;
        this.jumpTo = jumpTo;
    }


    @Override
    public Label execute(ExecutionContext context) {
        return (context.getVariablevalue(this.getVariable()) == context.getVariablevalue(this.getSecondaryVariable())) ? jumpTo : FixedLabel.EMPTY;
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

        Variable z1 = new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber());
        Variable z2 = new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber());

        Label l1 = new LabelImp(ex.getLabelNumber());
        Label l2 = new LabelImp(ex.getLabelNumber());
        Label l3 = new LabelImp(ex.getLabelNumber());

        AssigmentInstruction assigment = new AssigmentInstruction(z1,this.getVariable(),this,label);
        AssigmentInstruction assigment2 = new AssigmentInstruction(z2,this.getSecondaryVariable(),this);
        JumpZeroInstruction jumpZero = new JumpZeroInstruction(z1,l3,this,l2);
        JumpZeroInstruction jumpZero2 = new JumpZeroInstruction(z2,l1,this);
        DecreaseInstruction decrease = new DecreaseInstruction(z1,this);
        DecreaseInstruction decrease2 = new DecreaseInstruction(z2,this);
        GoToLabelInstruction goToLabel = new GoToLabelInstruction(z1,l2,this);
        JumpZeroInstruction jumpZero3 = new JumpZeroInstruction(z2,jumpTo,this,l3);
        NeutralInstruction neutral = new NeutralInstruction(this.getVariable(),this,l1);


        this.getInstructionProgram().addInstruction(assigment);
        this.getInstructionProgram().addInstruction(assigment2);
        this.getInstructionProgram().addInstruction(jumpZero);
        this.getInstructionProgram().addInstruction(jumpZero2);
        this.getInstructionProgram().addInstruction(decrease);
        this.getInstructionProgram().addInstruction(decrease2);
        this.getInstructionProgram().addInstruction(goToLabel);
        this.getInstructionProgram().addInstruction(jumpZero3);
        this.getInstructionProgram().addInstruction(neutral);
    }

    @Override
    public String toDisplayString()
    {
        return String.format("[%-5s]  IF %s = %s GOTO %s",
                this.getLabel().getLabelRepresentation(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.getSecondaryVariable().getRepresentation().toLowerCase(),
                this.jumpTo.getLabelRepresentation());
    }

}
