package semulator.logic.api;

public enum InstructionData {

    INCREASE("INCREASE",1,"B",0),
    DECREASE("DECREASE",1,"B",0),
    NEUTRAL("NEUTRAL",0,"B",0),
    JUMP_NO_ZERO("JUMP_NOT_ZERO",2,"B",0),
    ZERO_VARIABLE("ZERO_VARIABLE", 1,"S",1),
    JUMP_EQUAL_CONSTANT ("JUMP_EQUAL_CONSTANT",2,"S",3),
    JUMP_ZERO("JUMP_ZERO",2,"S",2),
    GOTO_LABEL("GOTO_LABEL",1,"S",1),
    ASSIGNMENT("ASSIGNMENT",4,"S",2),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT",2,"S",2),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE",2,"S",3)
    ;

    private final String name;
    private final int cycles;
    private final String isBasic;
    private final int degree;

    InstructionData(String name, int cycles, String isBasic, int degree) {
        this.name = name;
        this.cycles = cycles;
        this.isBasic = isBasic;
        this.degree = degree;
    }

    public String getName() {
        return name;
    }

    public int cycles() {
        return cycles;
    }

    public String isBasic() {
        return isBasic;
    }

}
