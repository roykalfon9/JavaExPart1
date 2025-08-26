package semulator.logic.expansion;

public class ExpansionIdAllocator {

    int maxWorkVariable;
    int maxLabel;

    public ExpansionIdAllocator(int maxWorkVariable, int maxLabel)
    {
        this.maxWorkVariable = maxWorkVariable;
        this.maxLabel = maxLabel;
    }

    public int getWorkVariableNumber(){
        maxWorkVariable++;
        return this.maxWorkVariable;
    }

    public int getLabelNumber(){
        maxLabel++;
        return this.maxLabel;
    }
}
