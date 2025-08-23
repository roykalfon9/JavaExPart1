package semulator.logic.program;

import semulator.logic.api.Sinstruction;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableType;

import java.util.*;

public class SprogramImpl implements Sprogram {

    private final String name;
    private final List<Sinstruction> instructions;
    private final Set<Label> labels;

    public SprogramImpl(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
        this.labels = new LinkedHashSet<>();
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
        ///  ??
        return 0;
    }

    @Override
    public int calculateCycle() {
        ///  ??
        return 0;
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

}
