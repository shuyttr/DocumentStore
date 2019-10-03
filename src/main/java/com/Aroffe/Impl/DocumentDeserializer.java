package main.java.com.Aroffe.Impl;

import main.java.com.Aroffe.Document;
import main.java.com.Aroffe.DocumentStore;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.commons.codec.binary.Base64;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DocumentDeserializer implements JsonDeserializer<Document>
{
   /**
    * a deserializer has to convert from strings back into an object
    * @param jsonElement
    * @param type
    * @param jsonDeserializationContext
    * @return
    * @throws JsonParseException
    */
   @Override
   public Document deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException
   {
      String format = jsonElement.getAsJsonObject().get("format").getAsString();
      String uri = jsonElement.getAsJsonObject().get("uri").getAsString();
      String hash = jsonElement.getAsJsonObject().get("hash").getAsString();
      String map = jsonElement.getAsJsonObject().get("map").getAsString();

      jsonElement.getAsJsonObject().get("byte").getAsString();
      byte[] bytes = Base64.decodeBase64(jsonElement.getAsJsonObject().get("byte").getAsString());

      DocumentStore.CompressionFormat compressionFormat = DocumentStore.CompressionFormat.valueOf(format);
      URI uri1 = null;

      try {
         uri1 = new URI(uri);
      } catch(URISyntaxException u) {
         u.printStackTrace();
      }

      int hashCode = Integer.valueOf(hash);
      Document result = new DocumentImpl(bytes, hashCode, uri1, compressionFormat);

      result.setWordMap(this.getDeserializedMap(map));
      return result;
   }

   private Map<String, Integer> getDeserializedMap(String mapAsString)
   {
      Map<String, Integer> result = new HashMap<>();
      char[] temp = mapAsString.toCharArray();
      char[] chars = Arrays.copyOfRange(temp, 1, temp.length - 1);
      mapAsString = new String(chars).toLowerCase();
      String[] entries = mapAsString.split(", ");
      for(int i = 0; i < entries.length; i++)
      {
         String pair = entries[i];
         String[] keyValue = pair.split("=");
         result.put(keyValue[0], Integer.valueOf(keyValue[1]));
      }
      return result;
   }
}

