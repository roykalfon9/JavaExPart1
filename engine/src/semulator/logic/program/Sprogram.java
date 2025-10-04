package semulator.logic.program;

import semulator.logic.api.Sinstruction;
import semulator.logic.functions.Function;
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
    String stringLabelNamesWithExitLast();
    void setNumberInstructions();
    Sprogram expand(int degree);
    int getBasicInstructionNumber();
    int getSynteticInstructionNumber();
    void addCycles(int instructionCycles);
    void resetCycles();
    void addFunction(Function function);
    List<Function> getFunctions();
    List<Function> getUseFunctions();
}
