package main.java.com.Aroffe.Impl;

import main.java.com.Aroffe.Document;
import main.java.com.Aroffe.DocumentStore;
import com.google.gson.*;
import org.apache.commons.codec.binary.Base64;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * This is my custom serializer for Documents
 */
public class DocumentSerializer implements JsonSerializer<Document>
{

   /**
    * a serializer has to convert objects to strings, since what will be serialized is a JSON document, which is a string.
    * @param document, the doc to serialize
    * @param type, not sure
    * @param jsonSerializationContext, also not sure
    * @return a JsonElement
    */
   @Override
   public JsonElement serialize(Document document, Type type, JsonSerializationContext jsonSerializationContext)
   {
      JsonObject object = new JsonObject();

      DocumentStore.CompressionFormat format = document.getCompressionFormat();
      URI uri = document.getKey();
      String hash = Integer.toString(document.getDocumentHashCode());
      Map<String, Integer> map = document.getWordMap();

      Map<String, String> stringMap = new HashMap<>();
      for(String key : map.keySet()){
         stringMap.put(key.toLowerCase(), Integer.toString(map.get(key)));
      }

      //This is my use of the byte[] encoder/decoder, specified in the 3.2.2 of Requirement Doc
      JsonPrimitive jsonPrimitive = new JsonPrimitive(Base64.encodeBase64String(document.getDocument()));
      String byteString = jsonPrimitive.getAsString();

      object.addProperty("format", format.toString());
      object.addProperty("uri", uri.toASCIIString());
      object.addProperty("hash", hash);
      object.addProperty("map", stringMap.toString());
      object.addProperty("byte", byteString);

      return object;
   }

}
