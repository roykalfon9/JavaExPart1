package semulator.logic.variable;

import java.util.Objects;

public class VariableImpl implements Variable {

    private VariableType type;
    private final int number;

    public VariableImpl(VariableType type, int number) {
        this.type = type;
        this.number = number;
    }

    @Override
    public VariableType getType() {
        return type;
    }
    @Override
    public String getRepresentation() {
        return type.getVariableRepresentation(number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableImpl)) return false;
        VariableImpl that = (VariableImpl) o;
        return number == that.number && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, number);
    }

    @Override
    public String toString() {
        return getRepresentation();
    }

}
