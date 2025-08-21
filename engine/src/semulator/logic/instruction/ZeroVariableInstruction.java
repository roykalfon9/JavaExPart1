package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.label.LabelImp;
import semulator.logic.program.SprogramImpl;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;

public class ZeroVariableInstruction extends AbstractInstruction {

    public ZeroVariableInstruction(Variable variable) {
        super(InstructionData.ZERO_VARIABLE, variable);
        InitializeIProgramInstruction();
    }
    public ZeroVariableInstruction(Variable variable, Label label) {
        super(InstructionData.ZERO_VARIABLE, variable, label);
        InitializeIProgramInstruction();
    }

    @Override
    public Label execute(ExecutionContext context) {
        if (context.getVariablevalue(this.getVariable()) != 0)
        {
            ProgramExecutorImpl expandExecute = new ProgramExecutorImpl(this.getExpandProgram());
            context.updateVariable(this.getVariable(), expandExecute.run(context.getVariablevalue(this.getVariable())));
        }
        return FixedLabel.EMPTY;
    }

    private void InitializeIProgramInstruction ()
    {
        Label label = new LabelImp(1);
        DecreaseInstruction decrease = new DecreaseInstruction(this.getVariable(), label);
        JumpNotZeroInstruction jumpNotZero = new JumpNotZeroInstruction(this.getVariable(), label);
        this.getExpandProgram().addInstruction(decrease);
        this.getExpandProgram().addInstruction(jumpNotZero);
    }
}
