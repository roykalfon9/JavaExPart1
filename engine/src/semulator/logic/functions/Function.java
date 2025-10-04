package semulator.logic.functions;

import semulator.logic.label.Label;
import semulator.logic.program.Sprogram;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class Function {

    private Sprogram instructionProgram;
    private Sprogram instructionExecuteProgram;

    private final String name;
    private final String userName;
    private final Set<Label> labels;
    private List<Long> args = new ArrayList<>();

    public Function(String name, String userName,Sprogram instructionExecuteProgram,Sprogram instructionProgram) {
        this.name = name;
        this.userName = userName;
        this.labels = new LinkedHashSet<>();
        this.instructionExecuteProgram = instructionExecuteProgram;
        this.instructionProgram = instructionProgram;
    }

    public String getName() {
        return this.name;
    }

    public String getUsherName() {
        return this.userName;
    }

    public void addArg(long arg) {
        this.args.add(arg);
    }

    public int cycles() {return instructionExecuteProgram.calculateCycle();}

    public int degree() {return instructionExecuteProgram.calculateMaxDegree();}

    public Sprogram getInstructionExecuteProgram() {return this.instructionExecuteProgram;}

    public Sprogram getInstructionProgram() {
        return this.instructionProgram;
    }

    public String toDisplayString() {
        return this.userName;
    }

}

