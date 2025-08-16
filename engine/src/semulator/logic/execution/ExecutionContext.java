package semulator.logic.execution;

import semulator.logic.variable.Variable;

public interface ExecutionContext {

    long getVariablevalue(Variable v);
    void updateVariable(Variable v,Long value);
}
