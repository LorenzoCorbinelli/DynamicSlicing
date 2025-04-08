package corbinelli.lorenzo.dynamicslicing;

import android.os.Build;
import android.util.Log;

import java.io.InputStream;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XPosedModule implements IXposedHookLoadPackage  {
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.d("LSPosedDebug", "Hooking into " + lpparam.packageName);

        try {
            XposedHelpers.findAndHookMethod(
                    "java.net.URL",
                    lpparam.classLoader,
                    "openStream",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Log.d("LSPosedDebug", "openStream called: " + param.thisObject);
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String result = "---";
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                result = new String(((InputStream)param.getResult()).readAllBytes());
                            }
                            Log.d("LSPosedDebug", "Result: " + result);
                        }
                    }
            );
        } catch(Throwable e) {
            Log.e("LSPosedDebug", "LSPosed Error: " + Log.getStackTraceString(e));
        }
    }
}
