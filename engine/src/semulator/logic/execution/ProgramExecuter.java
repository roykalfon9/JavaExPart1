package semulator.logic.execution;

import semulator.logic.variable.Variable;

import java.util.Map;

public interface ProgramExecuter {

    long run(Long... input);
    Map<Variable, Long> VariableState();
}
