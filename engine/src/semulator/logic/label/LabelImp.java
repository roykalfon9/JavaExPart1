package semulator.logic.label;

public class LabelImp implements Label {

    private final int number;
    private final String label;

    public LabelImp(int number) {
        label = "L" + number;
        this.number = number;
    }

    public String getLabelRepresentation() {
        return label;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LabelImp)) return false;
        LabelImp other = (LabelImp) obj;
        return this.number == other.number;
    }
    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(number);
    }

    @Override
    public String toString() {
        return label;
    }

}
