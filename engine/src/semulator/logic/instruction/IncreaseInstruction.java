package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.label.Label;

public class IncreaseInstruction extends  AbstractInstruction {

    public IncreaseInstruction() {
        super(InstructionData.INCREASE);
    }
    public IncreaseInstruction(Label label) {
        super(InstructionData.INCREASE,label);
    }

    @Override
    public void execute() {

    }
}
