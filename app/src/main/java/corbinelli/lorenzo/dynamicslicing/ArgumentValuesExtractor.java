package corbinelli.lorenzo.dynamicslicing;

import android.util.Log;

import com.google.gson.JsonElement;

public class ArgumentValuesExtractor {

    private boolean flag = false;
    private final VariableName variableName = VariableName.getInstance();

    private String escapeJson(String json) {
        return json.replace("\"", "\\\"");
    }

    public void extractArgumentValues(Class<?> type, JsonElement jsonElement, StringBuilder args) {
        if (jsonElement.isJsonObject() || jsonElement.isJsonArray()) {
            if (!flag) {
                Log.i("LSPosedLog", "Gson gson = new Gson();");
                flag = true;
            }
            String varName = variableName.getVariableName();
            Log.i("LSPosedLog", type.getCanonicalName()
                    + " "
                    + varName
                    + "  = gson.fromJson(\""
                    + escapeJson(jsonElement.toString())
                    + "\", " + type.getCanonicalName() + ".class);");
            args.append(varName).append(", ");
        } else {    // it's a raw element
            args.append(jsonElement).append(", ");
        }
    }
}