package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.label.LabelImp;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;

public class JoToLabrelInstruction extends AbstractInstruction {

    Label GoTo;

    public JoToLabrelInstruction(Variable variable, Label GoTo) {
        super(InstructionData.GOTO_LABEL, variable);
        this.GoTo = GoTo;
        InitializeIProgramInstruction();
    }
    public JoToLabrelInstruction(Variable variable,Label GoTo, Label label) {
        super(InstructionData.GOTO_LABEL, variable, label);
        this.GoTo = GoTo;
        InitializeIProgramInstruction();
    }

    @Override
    public Label execute(ExecutionContext context) {
        return GoTo;
    }

    private void InitializeIProgramInstruction ()
    {
        Variable z1 =  new VariableImpl(VariableType.WORK,1);
        Label label = new LabelImp(1);
        IncreaseInstruction increase = new  IncreaseInstruction(z1);
        JumpNotZeroInstruction jumpNotZero = new JumpNotZeroInstruction(z1, GoTo);
        this.getExpandProgram().addInstruction(increase);
        this.getExpandProgram().addInstruction(jumpNotZero);
    }
}
