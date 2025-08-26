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

public class GoToLabelInstruction extends AbstractInstruction {



    public GoToLabelInstruction(Variable variable, Label GoTo)
    {
        super(InstructionData.GOTO_LABEL, variable);
        this.jumpTo = GoTo;
    }
    public GoToLabelInstruction(Variable variable, Label GoTo, Label label)
    {
        super(InstructionData.GOTO_LABEL, variable, label);
        this.jumpTo = GoTo;
    }
    public GoToLabelInstruction(Variable variable, Label GoTo, Sinstruction parentInstruction)
    {
        super(InstructionData.GOTO_LABEL, variable, parentInstruction);
        this.jumpTo = GoTo;
    }
    public GoToLabelInstruction(Variable variable, Label GoTo, Sinstruction parentInstruction, Label label )
    {
        super(InstructionData.GOTO_LABEL, variable, parentInstruction, label);
        this.jumpTo = GoTo;
    }


    @Override
    public Label execute(ExecutionContext context)
    {
        return jumpTo;
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

        Variable z1 =  new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber());

        IncreaseInstruction increase = new  IncreaseInstruction(z1,this,label);
        JumpNotZeroInstruction jumpNotZero = new JumpNotZeroInstruction(z1, jumpTo, this);
        this.getInstructionProgram().addInstruction(increase);
        this.getInstructionProgram().addInstruction(jumpNotZero);
    }

    @Override
    public String toDisplayString()
    {
        return String.format("#%d (%s) [%-5s] GOTO %s (%d)",
                this.getInstructionNumber(),
                this.isBasic(),
                this.getLabel().getLabelRepresentation(),
                this.jumpTo.getLabelRepresentation(),
                this.cycles());
    }

}
