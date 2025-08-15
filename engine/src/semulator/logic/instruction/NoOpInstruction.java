package semulator.logic.instruction;

import semulator.logic.API.InstructionData;

public class NoOpInstruction extends AbstractInstruction {

    public NoOpInstruction() {
        super(InstructionData.NO_OP);
    }

    @Override
    public void execute() {

    }

}
