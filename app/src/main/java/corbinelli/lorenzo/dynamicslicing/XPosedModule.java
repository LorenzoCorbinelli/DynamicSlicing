package corbinelli.lorenzo.dynamicslicing;

import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/*
  {
    "className": "java.net.URL",
    "methodName": "openStream",
    "parametersType": []
  }
  {
    "className": "android.net.Uri.Builder",
    "methodName": "build",
    "parametersType": []
  }
  {
    "className": "okhttp3.OkHttpClient",
    "methodName": "newCall",
    "parametersType": ["okhttp3.Request"]
  }
  {
    "className": "javax.crypto.spec.SecretKeySpec",
    "parametersType": ["[B", "java.lang.String"]
  }
  {
    "className": "javax.crypto.Mac",
    "methodName": "init",
    "parametersType": ["java.security.Key"]
  }
  {
    "className": "javax.crypto.Mac",
    "methodName": "doFinal",
    "parametersType": ["[B"]
  }
 */

public class XPosedModule implements IXposedHookLoadPackage {

    private static final String LOG_TAG = "LSPosedLog";
    private final Gson gson = new Gson();

    private JSONArray readJSON() throws IOException, JSONException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("res/raw/hooks.json");
        String json = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new JSONArray(json);
    }

    private void extractArgumentValues(Class<?> type, JsonElement jsonElement, StringBuilder args) {
        if (jsonElement.isJsonObject()) {   // it's an object
            Log.d("LSPosedDebug", "TYPE: " + type);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            args.append("new ").append(type.getSimpleName()).append("(");  // object's type
            for (String key : jsonObject.keySet()) {
                try {
                    Class<?> argumentType = type.getDeclaredField(key).getType();    // get the next argument's type
                    extractArgumentValues(argumentType, jsonObject.get(key), args);
                } catch(Exception e) {
                    Log.e("LSPosedDebug", "ArgumentValues error: " + Log.getStackTraceString(e));
                }
            }
            // take off the last comma and space
            args.setLength(args.length() - 2);
            args.append("), ");
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            args.append("new ").append(type.getSimpleName()).append("{");
            for (int i = 0; i < jsonArray.size(); i++) {
                extractArgumentValues(type.getComponentType(), jsonArray.get(i), args);
            }
            args.setLength(args.length() - 2);
            args.append("}").append(", ");
        } else {    // it's a raw element
            if (type.isArray()) {   // it's an array of primitives
                //args.append("new ").append(type.getComponentType().getSimpleName()).append("()");
                args.append("new ")
                        .append(type.getSimpleName())
                        .append("{")
                        .append(jsonElement.toString().substring(1, jsonElement.toString().length() - 1))
                        .append("}")
                        .append(", ");
            } else {
                args.append(jsonElement).append(", ");
            }
        }
    }

    private void addCallback(Object[] parametersAndHook) {
        parametersAndHook[parametersAndHook.length - 1] = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Member hockedMember = param.method;
                String returnType;
                if (hockedMember instanceof Method) {
                    returnType = ((Method)hockedMember).getReturnType().getCanonicalName();
                } else {    // hockedMember is a constructor
                    returnType = hockedMember.getDeclaringClass().getCanonicalName() + " new ";
                }
                String memberName = hockedMember.getName();
                StringBuilder args = new StringBuilder();

                for (Object obj : param.args) {
                    JsonElement jsonElement = gson.toJsonTree(obj);
                    Log.d("LSPosedDebug", jsonElement.toString());
                    extractArgumentValues(obj.getClass(), jsonElement, args);
                }
                // take off the last comma and space
                args.setLength(args.length() - 2);
                Log.i(LOG_TAG, returnType + " " + memberName + "(" + args + ")");
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // only if the hooked member is a method
                if (param.method instanceof Method) {
                    Log.i(LOG_TAG, "Result: " + gson.toJson(param.getResult()));
                }
            }
        };
    }

    private Object[] getParametersTypeAndCallback(JSONObject hook) throws JSONException {
        JSONArray jsonParameters = hook.getJSONArray("parametersType");
        Object[] parametersAndHook = new Object[jsonParameters.length() + 1];   // as the last element will be the callback
        for (int i = 0; i < jsonParameters.length(); i++) {
            // take each parameterType as string
            parametersAndHook[i] = jsonParameters.getString(i);
        }
        addCallback(parametersAndHook);
        return parametersAndHook;
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.d("LSPosedDebug", "Hooking into " + lpparam.packageName);

        try {
            JSONArray jsonArray = readJSON();

            for (int i = 0; i < jsonArray.length(); i++) {
                // take each element in the json
                JSONObject hook = jsonArray.getJSONObject(i);
                Object[] parametersAndHook = getParametersTypeAndCallback(hook);

                if (hook.has("methodName")) {
                    XposedHelpers.findAndHookMethod(
                            hook.getString("className"),
                            lpparam.classLoader,
                            hook.getString("methodName"),
                            parametersAndHook
                    );
                } else {
                    XposedHelpers.findAndHookConstructor(
                            hook.getString("className"),
                            lpparam.classLoader,
                            parametersAndHook
                    );
                }

            }
        } catch(Throwable e) {
            Log.e("LSPosedDebug", "LSPosed Error: " + Log.getStackTraceString(e));
        }
    }
}
