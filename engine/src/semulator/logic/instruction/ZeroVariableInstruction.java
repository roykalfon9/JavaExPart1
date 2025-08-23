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
            ProgramExecutorImpl expandExecute = new ProgramExecutorImpl(this.getInstructionProgram());
            context.updateVariable(this.getVariable(), expandExecute.run(context.getVariablevalue(this.getVariable())));
        }
        return FixedLabel.EMPTY;
    }

    private void InitializeIProgramInstruction ()
    {
        Label label = new LabelImp(1);
        DecreaseInstruction decrease = new DecreaseInstruction(this.getVariable(),this, label);
        JumpNotZeroInstruction jumpNotZero = new JumpNotZeroInstruction(this.getVariable(), label,this);
        this.getInstructionProgram().addInstruction(decrease);
        this.getInstructionProgram().addInstruction(jumpNotZero);
    }
        public String toDisplayString()
        {
            return String.format("#%d (%s) [ %-5s ]  %s <- 0 (%d)",
                    this.getInstructionNumber(),
                    this.isBasic(),
                    this.getLabel().getLabelRepresentation(),
                    this.getVariable().getRepresentation().toLowerCase(),
                    this.cycles());
        }
    }

