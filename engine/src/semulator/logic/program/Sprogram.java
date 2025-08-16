package semulator.logic.program;

import semulator.logic.api.SInstruction;

import java.util.List;

public interface Sprogram {

    String getName();
    void addInstruction(SInstruction instruction);
    List<SInstruction> getInstructions();

    boolean validate();
    int calculateMaxDegree();
    int calculateCycle();
}
