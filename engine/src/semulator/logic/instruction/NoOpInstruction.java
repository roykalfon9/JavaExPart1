package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.label.Label;

public class NoOpInstruction extends AbstractInstruction {

    public NoOpInstruction() {
        super(InstructionData.NO_OP);
    }
    public NoOpInstruction(Label label) {
        super(InstructionData.NO_OP,label);
    }


    @Override
    public void execute() {

    }

}
