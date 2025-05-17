package corbinelli.lorenzo.dynamicslicing;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ArgumentValuesExtractor {

    private void extractObject(Class<?> type, JsonObject jsonObject, StringBuilder args) {
        args.append("new ").append(type.getSimpleName()).append("(");  // object's type
        for (String key : jsonObject.keySet()) {
            try {
                Class<?> argumentType = type.getDeclaredField(key).getType();    // get the field's type
                extractArgumentValues(argumentType, jsonObject.get(key), args);
            } catch (Exception e) {
                Log.e("LSPosedDebug", "ArgumentValues error: " + Log.getStackTraceString(e));
            }
        }
        // take off the last comma and space
        args.setLength(args.length() - 2);
        args.append("), ");
    }

    private void extractArray(Class<?> type, JsonArray jsonArray, StringBuilder args) {
        args.append("new ").append(type.getSimpleName()).append("{"); // array's type
        for (int i = 0; i < jsonArray.size(); i++) {
            extractArgumentValues(type.getComponentType(), jsonArray.get(i), args);
        }
        args.setLength(args.length() - 2);
        args.append("}").append(", ");
    }

    public void extractArgumentValues(Class<?> type, JsonElement jsonElement, StringBuilder args) {
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            extractObject(type, jsonObject, args);
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            extractArray(type, jsonArray, args);
        } else {    // it's a raw element
            args.append(jsonElement).append(", ");
        }
    }
}