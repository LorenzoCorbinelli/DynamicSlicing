package corbinelli.lorenzo.dynamicslicing;

import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JSONReader {

    private String path;

    public JSONReader(String path) {
        this.path = path;
    }

    public JSONArray readJSON() throws IOException, JSONException {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            String json = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            return new JSONArray(json);
        }
    }
}