package semulator.logic.execution;

import semulator.logic.api.SInstruction;
import semulator.logic.label.Label;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;

import java.util.HashMap;
import java.util.Map;

public class ProgramExecutorImpl implements ProgramExecuter{

    private final Sprogram program;
    private Map<Variable, Long> programVariableState =  new HashMap<>();

    public ProgramExecutorImpl(Sprogram program){
        this.program = program;
    }

    @Override
    public long run(Long... input) {

        for (int i = 0; i < input.length; i++) {
            programVariableState.put(new VariableImpl(VariableType.INPUT, i + 1), input[i]);
        }
        programVariableState.put(Variable.RESULT, 0L);


        ExecutionContext context = new ExecutionContextImpl(programVariableState);

        SInstruction currentInstruction = program.getInstructions().get(0);
        Label nextLabe;
        do{
            nextLabe = currentInstruction.execute(context);
        }
    }

    @Override
    public Map<Variable, Long> VariableState() {
        return programVariableState;
    }
}
