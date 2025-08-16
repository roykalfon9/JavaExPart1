package semulator.logic.variable;

public enum VariableType {
    RESULT{
        @Override
        public String getVariableRepresentation(int number){
            return ("y");
        };
    },
    INPUT{
        @Override
        public String getVariableRepresentation(int number){
            return ("x" + number);
        }
    },
    WORK{
        @Override
        public String getVariableRepresentation(int number){
            return ("y" + number);
        }
    };

    public abstract String getVariableRepresentation (int number);
}
