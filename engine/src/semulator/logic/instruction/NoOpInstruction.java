package semulator.logic.instruction;

import semulator.logic.api.InstructionData;

public class NoOpInstruction extends AbstractInstruction {

    public NoOpInstruction() {
        super(InstructionData.NO_OP);
    }

    @Override
    public void execute() {

    }

}
