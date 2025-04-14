package corbinelli.lorenzo.dynamicslicing;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class XPosedModule implements IXposedHookLoadPackage {

    private JSONArray readJSON() throws IOException, JSONException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("res/raw/hooks.json");
        String json = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new JSONArray(json);
    }

    private void addCallback(Object[] parametersAndHook) {
        parametersAndHook[parametersAndHook.length - 1] = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.d("LSPosedDebug", "Args: " + Arrays.toString(param.args));
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Log.d("LSPosedDebug", "Result: " + param.getResult());
            }
        };
    }

    private Object[] getParametersTypeAndCallback(JSONObject hook) throws JSONException {
        JSONArray jsonParameters = hook.getJSONArray("parametersType");
        Object[] parametersAndHook = new Object[jsonParameters.length() + 1];   // as the last element will be the callback
        for (int i = 0; i < jsonParameters.length(); i++) {
            // take ech parameterType as string
            String parameterType = jsonParameters.getString(i);
            try {
                parametersAndHook[i] = Class.forName(parameterType);
            } catch (ClassNotFoundException e) {
                // TODO: generalize this check with other types
                switch (parameterType) {
                    case "byte[]":
                        parametersAndHook[i] = byte[].class;
                        break;
                    case "long":
                        parametersAndHook[i] = long.class;
                        break;
                    default:
                        // in case there's not a reference of the class it will be specified as a string
                        parametersAndHook[i] = parameterType;
                }
            }
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
