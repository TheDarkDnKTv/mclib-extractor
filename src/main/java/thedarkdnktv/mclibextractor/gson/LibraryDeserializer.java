package thedarkdnktv.mclibextractor.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import thedarkdnktv.mclibextractor.model.Library;

import java.lang.reflect.Type;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LibraryDeserializer implements JsonDeserializer<Library> {

    @Override
    public Library deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonObject()) {
            var obj = json.getAsJsonObject();
            var name = obj.get("name").getAsString();
            Path path = null;

            json = obj.get("downloads");
            if (json != null && json.isJsonObject()) {
                json = json.getAsJsonObject().get("artifact");
                if (json != null && json.isJsonObject()) {
                    json = json.getAsJsonObject().get("path");
                    if (json != null) {
                        try {
                            path = Paths.get(json.getAsString());
                        } catch (InvalidPathException e) {
                            throw new JsonParseException(e);
                        }
                    }
                }
            }

            if (path != null) {
                return new Library(name, path);
            }
        }

        return null;
    }
}
