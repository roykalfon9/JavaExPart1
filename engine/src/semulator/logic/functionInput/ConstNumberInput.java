package semulator.logic.functionInput;

import semulator.logic.execution.ExecutionContext;
import semulator.logic.execution.ExecutionContextImpl;

public class ConstNumberInput implements FunctionInput {

    Long value;

    public ConstNumberInput(Long value){
        this.value = value;
    }

    @Override
    public Long getValue(ExecutionContext context) {
        return value;
    }

    @Override
    public String toDisplayString() {

    }
}
