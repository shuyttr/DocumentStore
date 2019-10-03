package main.java.com.Aroffe.Impl;

import com.google.gson.*;
import org.apache.commons.codec.binary.Base64;

import java.lang.reflect.Type;

class ByteSerializer implements JsonSerializer<byte[]>
{
   @Override
   public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(Base64.encodeBase64String(src));
   }

}
