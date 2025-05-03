package corbinelli.lorenzo.dynamicslicing;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
    "parametersType": ["byte[]", "java.lang.String"]
  }
  {
    "className": "javax.crypto.Mac",
    "methodName": "init",
    "parametersType": ["java.security.Key"]
  }
  {
    "className": "javax.crypto.Mac",
    "methodName": "doFinal",
    "parametersType": ["byte[]"]
  }
 */

public class XPosedModule implements IXposedHookLoadPackage {

    private static final String LOG_TAG = "LSPosedLog";

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
                Member hockedMember = param.method;
                String returnType;
                if (hockedMember instanceof Method) {
                    returnType = ((Method)hockedMember).getReturnType().getCanonicalName();
                } else {    // hockedMember is a constructor
                    returnType = hockedMember.getDeclaringClass().getCanonicalName();
                }
                String memberName = hockedMember.getName();
                String args;
                if (param.args.length == 0) {
                    args = "";
                } else {
                    args = Arrays.deepToString(param.args);
                }
                Log.i(LOG_TAG, returnType + " " + memberName + "(" + args + ")");
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Log.i(LOG_TAG, "Result: " + param.getResult());
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
