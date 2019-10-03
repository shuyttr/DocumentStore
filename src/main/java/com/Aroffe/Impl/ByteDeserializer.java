package main.java.com.Aroffe.Impl;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.commons.codec.binary.Base64;

import java.lang.reflect.Type;

class ByteDeserializer
{
   byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
   {
      return Base64.decodeBase64(json.getAsString());
   }

}
