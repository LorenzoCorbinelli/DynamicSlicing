package corbinelli.lorenzo.dynamicslicing;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class Serializer {

    private boolean flag = false;
    private final VariableName variableName = VariableName.getInstance();
    private final Gson gson = new Gson();
    private final String logTag;

    public Serializer(String logTag) {
        this.logTag = logTag;
    }

    private String escapeJson(String json) {
        return json.replace("\"", "\\\"");
    }

    /**
     * This method logs the creation of the object representing the given argument
     * and it adds to the StringBuilder the variable name created for the given argument
     * @param arg A method's argument to log
     * @param args The StringBuilder in which are saved the method's arguments names to log
     */
    public void extractArgumentValues(Object arg, StringBuilder args) {
        JsonElement jsonElement = gson.toJsonTree(arg);
        if (jsonElement.isJsonObject() || jsonElement.isJsonArray()) {
            String varName = getVarNameAndLogSerialization(arg.getClass(), jsonElement);
            args.append(varName).append(", ");
        } else {    // it's a raw element
            args.append(jsonElement).append(", ");
        }
    }

    /**
     * This method logs the creation of the given object
     * @param obj The object to log
     * @return The variable name corresponding to the object created
     */
    public String logObjectSerialization(Object obj) {
        JsonElement jsonElement = gson.toJsonTree(obj);
        return getVarNameAndLogSerialization(obj.getClass(), jsonElement);
    }

    /**
     * This method serialize the given object in a string representation
     * @param obj The object to serialize
     * @return The string representation of the object
     */
    public String serializeObject(Object obj) {
        return gson.toJson(obj);
    }

    private String getVarNameAndLogSerialization(Class<?> type, JsonElement jsonElement) {
        if (!flag) {
            Log.i(logTag, "Gson gson = new Gson();");
            flag = true;
        }
        String varName = variableName.getVariableName();
        Log.i(logTag, type.getCanonicalName()
                + " "
                + varName
                + "  = gson.fromJson(\""
                + escapeJson(jsonElement.toString())
                + "\", " + type.getCanonicalName() + ".class);");
        return varName;
    }
}