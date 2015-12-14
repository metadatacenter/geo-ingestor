package org.metadatacenter.repository.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.gsonfire.PostProcessor;

import java.util.List;
import java.util.Optional;

/**
 * GSON Fire directives in the {@link MetadataTemplateElement} class  exclude the
 * {@link MetadataTemplateElement#jsonLDTypes} and {@link MetadataTemplateElement#jsonLDIdentifier} fields from direct
 * serialization. Here we serialize them as JSON-LD-conforming <tt>@type</tt> and <tt>@id</tt> fields.
 * </p>
 * Note that the corresponding deserialization logic has not been implemented.
 *
 * @see MetadataTemplateElement
 * @see MetadataTemplateJSONSerializer
 */
public class MetadataTemplateElementPostProcessor implements PostProcessor<MetadataTemplateElement>
{
  @Override public void postDeserialize(MetadataTemplateElement metadataTemplateElement, JsonElement jsonElement,
    Gson gson)
  {
  }

  @Override public void postSerialize(JsonElement jsonElement, MetadataTemplateElement metadataTemplateElement,
    Gson gson)
  {
    if (jsonElement.isJsonObject()) {
      JsonObject obj = jsonElement.getAsJsonObject();

      if (obj.has("jsonLDContext"))
        obj.remove("jsonLDContext");

      if (obj.has("jsonLDTypes"))
        obj.remove("jsonLDTypes");

      if (obj.has("jsonLDIdentifier"))
        obj.remove("jsonLDIdentifier");

      Optional<JSONLDContext> jsonLDContext = metadataTemplateElement.getJSONLDContext();
      List<String> jsonLDTypes = metadataTemplateElement.getJSONLDTypes();
      Optional<String> jsonLDIdentifier = metadataTemplateElement.getJSONLDIdentifier();

      if (jsonLDContext.isPresent()) {
        List<JSONLDContextEntry> jsonLDContextEntries = jsonLDContext.get().getJSONLDContextEntries();
        JsonObject contextObject = new JsonObject();
        for (int contextValueIndex = 0; contextValueIndex < jsonLDContextEntries.size(); contextValueIndex++) {
          JSONLDContextEntry jsonLDContextEntry = jsonLDContextEntries.get(contextValueIndex);
          contextObject.addProperty(jsonLDContextEntry.getPropertyName(), jsonLDContextEntry.getPropertyURI());
        }
        obj.add("@Context", contextObject);
      }

      if (jsonLDIdentifier.isPresent())
        obj.addProperty("@id", jsonLDIdentifier.get());

      if (!jsonLDTypes.isEmpty()) {
        if (jsonLDTypes.size() == 1)
          obj.addProperty("@type", jsonLDTypes.get(0));
        else {
          JsonArray jsonLDTypesArray = new JsonArray();
          for (int typeValueIndex = 0; typeValueIndex < jsonLDTypes.size(); typeValueIndex++) {
            JsonElement jsonLDType = new JsonPrimitive(jsonLDTypes.get(typeValueIndex));
            jsonLDTypesArray.add(jsonLDType);
          }
          obj.add("@type", jsonLDTypesArray);
        }
      }
    }
  }
}
