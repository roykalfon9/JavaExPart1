package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.label.Label;
import semulator.logic.label.LabelImp;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;

public class JoToLabelInstruction extends AbstractInstruction {

    public JoToLabelInstruction(Variable variable, Label GoTo) {
        super(InstructionData.GOTO_LABEL, variable);
        this.jumpTo = GoTo;
        InitializeIProgramInstruction();
    }
    public JoToLabelInstruction(Variable variable, Label GoTo, Label label) {
        super(InstructionData.GOTO_LABEL, variable, label);
        this.jumpTo = GoTo;
        InitializeIProgramInstruction();
    }
    public JoToLabelInstruction(Variable variable, Label GoTo, Sinstruction parentInstruction)
    {
        super(InstructionData.GOTO_LABEL, variable, parentInstruction);
        this.jumpTo = GoTo;
        InitializeIProgramInstruction();
    }
    public JoToLabelInstruction(Variable variable, Label GoTo, Sinstruction parentInstruction, Label label )
    {
        super(InstructionData.GOTO_LABEL, variable, parentInstruction, label);
        this.jumpTo = GoTo;
        InitializeIProgramInstruction();
    }


    @Override
    public Label execute(ExecutionContext context)
    {
        return jumpTo;
    }

    private void InitializeIProgramInstruction ()
    {
        Variable z1 =  new VariableImpl(VariableType.WORK,1);
        Label label = new LabelImp(1);
        IncreaseInstruction increase = new  IncreaseInstruction(z1,this,this.getLabel());
        JumpNotZeroInstruction jumpNotZero = new JumpNotZeroInstruction(z1, jumpTo);
        this.getInstructionProgram().addInstruction(increase);
        this.getInstructionProgram().addInstruction(jumpNotZero);
    }

    @Override
    public String toDisplayString()
    {
        return String.format("#%d (%s) [ %-5s ] GOTO %s (%d)",
                this.getInstructionNumber(),
                this.isBasic(),
                this.getLabel().getLabelRepresentation(),
                this.jumpTo.getLabelRepresentation(),
                this.cycles());
    }

}
