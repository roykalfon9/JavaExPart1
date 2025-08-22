package semulator.logic.program;

import semulator.logic.api.Sinstruction;
import semulator.logic.label.Label;

import java.util.List;

public interface Sprogram {

    String getName();
    void addInstruction(Sinstruction instruction);
    List<Sinstruction> getInstructions();

    int findInstructionIndexByLabel(Label label);
    boolean validate();
    int calculateMaxDegree();
    int calculateCycle();
    String stringInputVariable();
    void setNumberInstructions();
}
