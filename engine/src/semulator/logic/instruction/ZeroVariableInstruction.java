package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.expansion.ExpansionIdAllocator;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.label.LabelImp;
import semulator.logic.program.SprogramImpl;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;

public class ZeroVariableInstruction extends AbstractInstruction {

    public ZeroVariableInstruction(Variable variable) {
        super(InstructionData.ZERO_VARIABLE, variable);
    }
    public ZeroVariableInstruction(Variable variable, Label label) {
        super(InstructionData.ZERO_VARIABLE, variable, label);
    }
    public ZeroVariableInstruction(Variable variable, Sinstruction parentInstruction) {
        super(InstructionData.ZERO_VARIABLE, variable, parentInstruction);
    }
    public ZeroVariableInstruction(Variable variable, Sinstruction parentInstruction, Label label) {
        super(InstructionData.ZERO_VARIABLE, variable, parentInstruction, label);
    }




    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(this.getVariable(), 0L);
        return FixedLabel.EMPTY;
    }

    @Override
    public void InitializeIProgramInstruction (ExpansionIdAllocator ex) {
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

        DecreaseInstruction decrease = new DecreaseInstruction(this.getVariable(),this, label);
        JumpNotZeroInstruction jumpNotZero = new JumpNotZeroInstruction(this.getVariable(), label,this);

        this.getInstructionProgram().addInstruction(decrease);
        this.getInstructionProgram().addInstruction(jumpNotZero);
    }
        public String toDisplayString()
        {
            return String.format("#%d (%s) [%-5s]  %s <- 0 (%d)",
                    this.getInstructionNumber(),
                    this.isBasic(),
                    this.getLabel().getLabelRepresentation(),
                    this.getVariable().getRepresentation().toLowerCase(),
                    this.cycles());
        }
    }

