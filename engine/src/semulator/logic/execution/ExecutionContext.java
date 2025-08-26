package semulator.logic.execution;

import semulator.logic.variable.Variable;

import java.util.Map;

public interface ExecutionContext {

    long getVariablevalue(Variable v);
    void updateVariable(Variable v,Long value);
    Map<Variable, Long> getContext();
}
