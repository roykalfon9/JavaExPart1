package semulator.logic.variable;

public interface Variable {
    VariableType getType();
    String getRepresentation();
    int getNumber();

    Variable RESULT = new VariableImpl(VariableType.RESULT, 0);
}
