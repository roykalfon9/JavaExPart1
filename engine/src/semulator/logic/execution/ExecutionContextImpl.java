package semulator.logic.execution;

import semulator.logic.variable.Variable;

import java.util.Map;

public class ExecutionContextImpl implements ExecutionContext {

    Map<Variable, Long> VariableState;

    public ExecutionContextImpl(Map<Variable, Long> VariableState) {
        this.VariableState = VariableState;
    }

    @Override
    public long getVariablevalue(Variable v) {
        return VariableState.getOrDefault(v, 0L);
    }

    @Override
    public void updateVariable(Variable v, Long value) {
        VariableState.put(v, value);
    }
}
