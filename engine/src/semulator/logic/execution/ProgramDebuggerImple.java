package semulator.logic.execution;

import semulator.logic.api.Sinstruction;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProgramDebuggerImple implements ProgramExecuter {

    private final Sprogram program;
    private Map<Variable, Long> programVariableState  = new LinkedHashMap<>();
    private ExecutionContext context;
    private int instructionIndex = 0;
    private boolean isOver = false;
    private Long result = 0L;

    private Variable lastVariableChange = null;
    private Variable lastSecondaryVariableChange = null; // ADDED

    public ProgramDebuggerImple(Sprogram program){
        this.program = program;
        insertRestartVariables();
        this.program.setNumberInstructions();
    }

    @Override
    public long run(Long... input)
    {
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
        context = new ExecutionContextImpl(programVariableState);
        isOver = false;
        instructionIndex = 0;
        lastVariableChange = null;
        lastSecondaryVariableChange = null;
        result = 0L;
        return 0;
    }

    public void stepOver()
    {
        Label nextLabe;

        if (program.getInstructions().isEmpty()) {
            this.isOver = true;
        }
        else {
            Sinstruction currentInstruction = program.getInstructions().get(instructionIndex);

            // שמירת ה"משתנים הרלוונטיים" של הפקודה האחרונה לצורך היילייט // ADDED
            this.lastVariableChange = currentInstruction.getVariable();
            this.lastSecondaryVariableChange = currentInstruction.getSecondaryVariable();

            nextLabe = currentInstruction.execute(context);
            this.program.addCycles(currentInstruction.cycles());


            if (nextLabe == FixedLabel.EMPTY) {
                instructionIndex++;
            } else if (nextLabe != FixedLabel.EXIT) {
                instructionIndex = program.findInstructionIndexByLabel(nextLabe);
            }

            if (instructionIndex == program.getInstructions().size() || nextLabe == FixedLabel.EXIT)
            {
                Variable y = new VariableImpl(VariableType.RESULT, 1);
                this.result = context.getVariablevalue(y);
                this.isOver = true;
            }
            programVariableState = context.getContext();
        }
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

    public List<String> getInputLabelsNames()
    {
        List<String> inputLabels = new ArrayList<>();
        for (Variable v : programVariableState.keySet()) {
            if (v.getType() == VariableType.INPUT && !inputLabels.contains(v.getRepresentation()))
                inputLabels.add(v.getRepresentation());
        }
        return inputLabels;
    }

    public int getInstructionIndex() {return instructionIndex;}
    public boolean isOver() {return isOver;}
    public Long getResult() {return result;}
    public Variable getLastVariableChange() {return lastVariableChange;}
    public Variable getLastSecondaryVariableChange() {return lastSecondaryVariableChange;} // ADDED
}
