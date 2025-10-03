package semulator.logic.program;

import semulator.logic.api.Sinstruction;
import semulator.logic.expansion.ExpansionIdAllocator;
import semulator.logic.functions.Function;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableType;

import java.util.*;

public class SprogramImpl implements Sprogram {

    private final String name;
    private final List<Sinstruction> instructions;
    private final List<Function> functions;
    private final Set<Label> labels;

    public SprogramImpl(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
        this.labels = new LinkedHashSet<>();
        this.functions = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addInstruction(Sinstruction instruction) {
        instructions.add(instruction);
        if (instruction.getLabel() != null && instruction.getLabel() != FixedLabel.EMPTY)
        {
            labels.add(instruction.getLabel());
        }
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    @Override
    public List<Sinstruction> getInstructions() {
        return instructions;
    }

    @Override
    public int findInstructionIndexByLabel(Label label)
    {
        if (label == null) throw new IllegalArgumentException("label is null");
        for (int i = 0; i < instructions.size(); i++) {
            Label current = instructions.get(i).getLabel();
            if (current != null && label.equals(current)) {
                return i;
            }
        }
        throw new NoSuchElementException("No instruction with label: " + label.getLabelRepresentation());
    }

    @Override
    public int getBasicInstructionNumber()
    {
        int num = 0;
        for (Sinstruction instruction : instructions) {
            if (instruction.isBasic().equalsIgnoreCase("B"))
            {
                num++;
            }
        }
        return num;
    }

    @Override
    public int getSynteticInstructionNumber()
    {
        int num = 0;
        for (Sinstruction instruction : instructions) {
            if(instruction.isBasic().equalsIgnoreCase("S"))
            {
                num++;
            }
        }
        return num;
    }

    @Override
    public boolean validate() {
        for (Sinstruction instruction : instructions)
        {
            if (instruction.getJumpLabel() == FixedLabel.EMPTY)
            {
                return false;
            }
            if (instruction.getJumpLabel() != null && instruction.getJumpLabel() != FixedLabel.EXIT && instruction.getJumpLabel() != FixedLabel.EMPTY && !labels.contains(instruction.getJumpLabel()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int calculateMaxDegree() {
        int maxDegree = 0;
        for (Sinstruction instruction : instructions)
        {
            if (instruction.degree() > maxDegree)
            {
                maxDegree = instruction.degree();
            }
        }
        return maxDegree;
    }

    @Override
    public int calculateCycle() {
        int totalCycles = 0;
        for (Sinstruction instruction : instructions)
        {
            totalCycles += instruction.cycles();
        }
        return totalCycles;
    }

    @Override
    public String stringInputVariable() {
        StringJoiner joiner = new StringJoiner(", ");
        LinkedHashSet<String> seen = new LinkedHashSet<>();

        for (Sinstruction instruction : instructions) {
            if (instruction == null || instruction.getVariable() == null) continue;
            Variable v = instruction.getVariable();

            if (v.getType() == VariableType.INPUT) {
                String token = v.getRepresentation();
                if (seen.add(token)) {
                    joiner.add(token);
                }
            }
             if (instruction.getSecondaryVariable() != null)
            {
                Variable s = instruction.getSecondaryVariable();
                String stoken = s.getRepresentation();
                if (seen.add(stoken)) {
                    joiner.add(stoken);
                }
            }
        }
        return joiner.toString();
    }

    @Override
    public String stringLabelNamesWithExitLast() {
        StringJoiner joiner = new StringJoiner(", ");
        boolean sawExit = false;

        if (labels == null || labels.isEmpty()) {
            return "";
        }

        for (Label lbl : labels) {
            if (lbl == null || lbl == FixedLabel.EMPTY) continue;
            if (lbl == FixedLabel.EXIT) {
                sawExit = true;
                continue;
            }
            joiner.add(lbl.getLabelRepresentation());
        }

        if (sawExit) {
            joiner.add("EXIT");
        }

        return joiner.toString();
    }


    @Override
    public void setNumberInstructions()
    {
        for(int i=0;i<this.instructions.size();i++)
        {
            this.instructions.get(i).setInstructionNumber(i+1);
        }
    }

    @Override
    public Sprogram expand(int degree)
    {
        ExpansionIdAllocator idAllocator = new ExpansionIdAllocator(getMaxWorkVariableNumber(), getMaxLabelNumber());
        int currDegree = 0;

        List<Sinstruction> expandInstructions = new ArrayList<>();
        List<Sinstruction> tempList = new ArrayList<>();

        expandInstructions.addAll(instructions);

        do{
            for(Sinstruction instruction : expandInstructions)
            {
                if (instruction.degree()>0)
                {
                    instruction.InitializeIProgramInstruction(idAllocator);

                    for(int i=0; i<instruction.getInstructionProgram().getInstructions().size();i++)
                    {
                        tempList.add(instruction.getInstructionProgram().getInstructions().get(i));
                    }
                }
                else
                {
                    tempList.add(instruction);
                }
            }
            expandInstructions.clear();
            expandInstructions.addAll(tempList);
            tempList.clear();
            currDegree++;
        }
        while (currDegree<degree);

        Sprogram expandProgram = new SprogramImpl(this.name);

        for(Sinstruction instruction : expandInstructions)
        {
            expandProgram.addInstruction(instruction);
        }

        expandProgram.setNumberInstructions();

        return expandProgram;
    }

    private int getMaxLabelNumber() {

        int max = 0;

        for (Sinstruction instruction : instructions) {
            if (instruction == null || instruction.getVariable() == null) continue;
            Label l = instruction.getLabel();
            if (l.getNumber() > max) {
                max = l.getNumber();
            }
        }
        return max;
    }

    private int getMaxWorkVariableNumber() {

        int max = 0;

        for (Sinstruction instruction : instructions) {
            if (instruction == null || instruction.getVariable() == null) continue;
                Variable v = instruction.getVariable();
                if (v.getType() == VariableType.WORK) {
                    if (v.getNumber() > max) {
                        max = v.getNumber();
                    }
                }
            }
        return max;
    }
}
