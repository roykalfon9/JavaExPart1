package semulator.logic.instruction;

import semulator.logic.API.InstructionData;
import semulator.logic.API.SInstruction;

public abstract class AbstractInstruction implements SInstruction {

    private final InstructionData instructionData;

    public AbstractInstruction(InstructionData instructionData) {
     this.instructionData = instructionData;
    }

    @Override
    public String getName() {
        return instructionData.getName();
    }

    @Override
    public int cycles() {
        return instructionData.cycles();
    }
}
