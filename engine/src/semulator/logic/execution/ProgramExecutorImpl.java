package semulator.logic.execution;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.instruction.QuoteInstruction;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;

import java.util.*;

public class ProgramExecutorImpl implements ProgramExecuter{

    private final Sprogram program;
    private Map<Variable, Long> programVariableState =  new LinkedHashMap<>();


    public ProgramExecutorImpl(Sprogram program){
        this.program = program;
        insertRestartVariables();
        this.program.setNumberInstructions();
        this.program.resetCycles();
    }

    @Override
    public long run(Long... input) {

        insertRestartVariables();
        this.program.resetCycles();

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
            this.program.addCycles(currentInstruction.cycles());

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
            Sinstruction ins = program.getInstructions().get(i);

            if (ins instanceof QuoteInstruction qi) {
                qi.addVar(programVariableState);
            } else {
                programVariableState.put(ins.getVariable(), 0L);
                if (ins.getSecondaryVariable() != null) {
                    programVariableState.put(ins.getSecondaryVariable(), 0L);
                }
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

    public List<String> getInputLabelsNames()
    {
        List<String> inputLabels = new ArrayList<>();


        for (Variable v : programVariableState.keySet()) {
            if (v.getType() == VariableType.INPUT && !inputLabels.contains(v.getRepresentation()))
                inputLabels.add(v.getRepresentation());
        }
        return inputLabels;
    }

}
