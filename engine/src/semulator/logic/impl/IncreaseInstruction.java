package semulator.logic.impl;

import semulator.logic.model.SInstruction;

public class IncreaseInstruction implements SInstruction {

    private final String name;
    private final int cycles;

    public IncreaseInstruction() {
        this.name = "Increase";
        this.cycles = 1;
    }

    @Override
    public String getName() {
        return "";
    }
    @Override
    public int cycles(){
        return 0;
    }
    @Override
    public void execute() {}
}
