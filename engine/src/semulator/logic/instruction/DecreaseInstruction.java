package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.label.Label;

public class DecreaseInstruction extends AbstractInstruction{

    public DecreaseInstruction() {
        super(InstructionData.DECREASE);
    }
    public DecreaseInstruction(Label label) {
        super(InstructionData.DECREASE, label);
    }

    @Override
    public void execute() {

    }
}
