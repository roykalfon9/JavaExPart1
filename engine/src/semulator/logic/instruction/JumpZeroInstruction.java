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

public class JumpZeroInstruction extends AbstractInstruction{

    public JumpZeroInstruction(Variable variable, Label jumpTargetIfTrue)
    {
        super(InstructionData.JUMP_ZERO, variable);
        this.jumpTo = jumpTargetIfTrue;
    }
    public JumpZeroInstruction(Variable variable, Label jumpTargetIfTrue, Label label)
    {
        super(InstructionData.JUMP_ZERO, variable, label);
        this.jumpTo = jumpTargetIfTrue;
    }
    public JumpZeroInstruction(Variable variable, Label jumpTargetIfTrue, Sinstruction parentInstruction)
    {
        super(InstructionData.JUMP_ZERO, variable, parentInstruction);
        this.jumpTo = jumpTargetIfTrue;
    }
    public JumpZeroInstruction(Variable variable, Label jumpTargetIfTrue, Sinstruction parentInstruction, Label label)
    {
        super(InstructionData.JUMP_ZERO, variable, parentInstruction, label);
        this.jumpTo = jumpTargetIfTrue;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return (context.getVariablevalue(this.getVariable()) == 0) ? jumpTo : FixedLabel.EMPTY;
    }

    @Override
    public void InitializeIProgramInstruction (ExpansionIdAllocator ex)  {
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

        Label falseLabel = new LabelImp(ex.getLabelNumber());

        JumpNotZeroInstruction jumpNotZero = new JumpNotZeroInstruction(this.getVariable(),falseLabel,this,label);
        GoToLabelInstruction goToLabel = new GoToLabelInstruction(this.getVariable(),this.jumpTo,this);
        NeutralInstruction neutral = new NeutralInstruction(this.getVariable(),this,falseLabel);

        this.getInstructionProgram().addInstruction(jumpNotZero);
        this.getInstructionProgram().addInstruction(goToLabel);
        this.getInstructionProgram().addInstruction(neutral);

    }

    @Override
    public String toDisplayString()
    {
        return String.format("#%d (%s) [%-5s] IF %s = 0 GOTO %s (%d)",
                this.getInstructionNumber(),
                this.isBasic(),
                this.getLabel().getLabelRepresentation(),
                this.getVariable().getRepresentation().toLowerCase(),
                this.jumpTo.getLabelRepresentation(),
                this.cycles());
    }


}
