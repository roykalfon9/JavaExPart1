package semulator.logic.program;

import semulator.logic.api.SInstruction;

import java.util.ArrayList;
import java.util.List;

public class SprogramImpl implements Sprogram {

    private final String name;
    private final List<SInstruction> instructions;

    public SprogramImpl(String name, List<SInstruction> instructions) {
        this.name = name;
        this.instructions = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addInstruction(SInstruction instruction) {
        instructions.add(instruction);
    }

    @Override
    public List<SInstruction> getInstructions() {
        return instructions;
    }

    @Override
    public boolean validate() {
        ///  ??
        return false;
    }

    @Override
    public int calculateMaxDegree() {
        ///  ??
        return 0;
    }

    @Override
    public int calculateCycle() {
        ///  ??
        return 0;
    }
}
