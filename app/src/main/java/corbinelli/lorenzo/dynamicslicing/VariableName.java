package corbinelli.lorenzo.dynamicslicing;

public final class VariableName {

    private int variableNumber = 0;
    private final String variableName = "x";
    private static VariableName INSTANCE;

    private VariableName() {}

    public static VariableName getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VariableName();
        }
        return INSTANCE;
    }

    public String getVariableName() {
        return variableName + variableNumber++;
    }
}
