package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.SInstruction;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;

public abstract class AbstractInstruction implements SInstruction {

    private final InstructionData instructionData;
    private final Label label;

    public AbstractInstruction(InstructionData instructionData) {
     this(instructionData, FixedLabel.EMPTY);
    }

    public AbstractInstruction(InstructionData instructionData, Label label) {
        this.instructionData = instructionData;
        this.label = label;
    }


    @Override
    public String getName() {
        return instructionData.getName();
    }

    @Override
    public int cycles() {
        return instructionData.cycles();
    }

    @Override
    public Label getLabel() {
        return  label;
    }
}
