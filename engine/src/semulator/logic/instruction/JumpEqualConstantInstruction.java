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

public class JumpEqualConstantInstruction extends AbstractInstruction{

    public JumpEqualConstantInstruction(Variable variable, Label jumpTo, long constValue)
    {
        super(InstructionData.JUMP_EQUAL_CONSTANT, variable);
        this.constValue = constValue;
        this.jumpTo = jumpTo;
    }
    public JumpEqualConstantInstruction(Variable variable, Label jumpTo, long constValue, Label label)
    {
        super(InstructionData.JUMP_EQUAL_CONSTANT, variable, label);
        this.constValue = constValue;
        this.jumpTo = jumpTo;
    }
    public JumpEqualConstantInstruction(Variable variable, Label jumpTo, long constValue, Sinstruction parentInstruction)
    {
        super(InstructionData.JUMP_EQUAL_CONSTANT, variable, parentInstruction);
        this.constValue = constValue;
        this.jumpTo = jumpTo;
    }
    public JumpEqualConstantInstruction(Variable variable, Label jumpTo, long constValue, Sinstruction parentInstruction, Label label )
    {
        super(InstructionData.JUMP_EQUAL_CONSTANT, variable, parentInstruction, label);
        this.constValue = constValue;
        this.jumpTo = jumpTo;
    }


    @Override
    public Label execute(ExecutionContext context) {
        return (context.getVariablevalue(this.getVariable()) == this.constValue) ? jumpTo : FixedLabel.EMPTY;
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
        Label falseLabel = new LabelImp(ex.getLabelNumber());

        AssigmentInstruction assigment = new AssigmentInstruction(z1,this.getVariable(),this,label);
        //JumpZeroInstruction jumpZero = new JumpZeroInstruction(z1,falseLabel,this);
        //DecreaseInstruction decrease = new DecreaseInstruction(z1,this);
        JumpNotZeroInstruction jumpNotZero = new JumpNotZeroInstruction(z1,falseLabel,this);
        GoToLabelInstruction goToLabel = new GoToLabelInstruction(z1,jumpTo,this);
        NeutralInstruction neutral = new NeutralInstruction(this.getVariable(),this,falseLabel);

        this.getInstructionProgram().addInstruction(assigment);

        for (long i=0; i<this.getConstValue();i++)
        {
            JumpZeroInstruction jumpZero = new JumpZeroInstruction(z1,falseLabel,this);
            DecreaseInstruction decrease = new DecreaseInstruction(z1,this);
            this.getInstructionProgram().addInstruction(jumpZero);
            this.getInstructionProgram().addInstruction(decrease);
        }

        this.getInstructionProgram().addInstruction(jumpNotZero);
        this.getInstructionProgram().addInstruction(goToLabel);
        this.getInstructionProgram().addInstruction(neutral);
    }

    @Override
    public String toDisplayString()
    {
        return String.format("#%d (%s) [%-5s] IF %s = %d GOTO %s (%d)",
                this.getInstructionNumber(),
                this.isBasic(),
                this.getLabel().getLabelRepresentation(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.constValue,
                this.jumpTo.getLabelRepresentation(),
                this.cycles());
    }

}
