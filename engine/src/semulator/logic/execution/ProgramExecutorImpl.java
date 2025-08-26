package semulator.logic.execution;

import semulator.logic.api.Sinstruction;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProgramExecutorImpl implements ProgramExecuter{

    private final Sprogram program;
    private Map<Variable, Long> programVariableState =  new LinkedHashMap<>();


    public ProgramExecutorImpl(Sprogram program){
        this.program = program;
        insertRestartVariables();
        this.program.setNumberInstructions();
    }

    @Override
    public long run(Long... input) {

        insertRestartVariables();
        if (input == null) input = new Long[0];

        int j = 0;
        for (Variable v : programVariableState.keySet()) {
            if (v.getType() == VariableType.INPUT && j < input.length ) {
                programVariableState.put(v, input[j]);
                j++;
            }
        }


        ExecutionContext context = new ExecutionContextImpl(programVariableState);

        Integer instructionIndex = 0;
        Label nextLabe;

        if (program.getInstructions().isEmpty()) {
            return context.getVariablevalue(Variable.RESULT);
        }

        do{
            Sinstruction currentInstruction = program.getInstructions().get(instructionIndex);
            nextLabe = currentInstruction.execute(context);

            if (nextLabe == FixedLabel.EMPTY){
                instructionIndex++;
                // next instruction
            }
            else if (nextLabe != FixedLabel.EXIT){
                instructionIndex = program.findInstructionIndexByLabel(nextLabe);
                // do find instruction by label in program
            }

        } while (nextLabe != FixedLabel.EXIT && instructionIndex < program.getInstructions().size()) ;

        Variable y = new VariableImpl(VariableType.RESULT, 1);

        programVariableState = context.getContext();

        return context.getVariablevalue(y);
    }

    @Override
    public Map<Variable, Long> VariableState() {
        return programVariableState;
    }

    private void insertRestartVariables()
    {
        programVariableState.clear();
        for (int i = 0; i < program.getInstructions().size(); i++)
        {
            programVariableState.put(program.getInstructions().get(i).getVariable(), 0L);
            if (program.getInstructions().get(i).getSecondaryVariable() != null)
            {
                programVariableState.put(program.getInstructions().get(i).getSecondaryVariable(), 0L);
            }
        }
            Variable y = new VariableImpl(VariableType.RESULT, 1);
            programVariableState.put(y, 0L);
    }


    private boolean IsReturnVariableExist()
    {
        for (Variable v : programVariableState.keySet()) {
            if (v.getType() == VariableType.RESULT) return true;
        }
        return false;
    }
}
