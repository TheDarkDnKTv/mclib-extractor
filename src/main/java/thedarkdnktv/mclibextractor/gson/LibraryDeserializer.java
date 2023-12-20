package thedarkdnktv.mclibextractor.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import thedarkdnktv.mclibextractor.model.Library;

import java.lang.reflect.Type;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class LibraryDeserializer implements JsonDeserializer<Library> {

    @Override
    public Library deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonObject()) {
            var obj = json.getAsJsonObject();
            var result = new Library(obj.get("name").getAsString());

            json = obj.get("downloads");
            if (json != null && json.isJsonObject()) {
                json = json.getAsJsonObject().get("artifact");
                if (json != null && json.isJsonObject()) {
                    var pathJson = json.getAsJsonObject().get("path");
                    var urlJson = json.getAsJsonObject().get("url");

                    if (pathJson != null) {
                        try {
                            result.setPath(Paths.get(pathJson.getAsString()));
                        } catch (InvalidPathException e) {
                            throw new JsonParseException(e);
                        }
                    }

                    if (urlJson != null) {
                        try {
                            result.setUrl(urlJson.getAsString());
                        } catch (InvalidPathException e) {
                            throw new JsonParseException(e);
                        }
                    }
                }
            }

            json = obj.get("rules");
            if (json != null && json.isJsonArray()) {
                var optional = json.getAsJsonArray()
                        .asList()
                        .stream()
                        .filter(JsonElement::isJsonObject)
                        .map(JsonElement::getAsJsonObject)
                        .filter(e -> e.has("os"))
                        .findFirst();
                result.setNative(optional.isPresent());
            }

            if (result.getPath() != null) {
                return result;
            }
        }

        return null;
    }
}
