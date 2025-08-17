package semulator.logic.api;

public enum InstructionData {

    INCREASE("INCREASE",1),
    DECREASE("DECREASE",1),
    NO_OP("NO OP",0),
    JUMP_NO_ZERO("JNZ",2)

    ;

    private final String name;
    private final int cycles;

    InstructionData(String name, int cycles) {
        this.name = name;
        this.cycles = cycles;
    }

    public String getName() {
        return name;
    }

    public int cycles() {
        return cycles;
    }

}
