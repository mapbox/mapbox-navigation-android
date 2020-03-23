package com.mapbox.navigation.core;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.mapbox.navigation.core.telemetry.events.NavigationArriveEvent;
import com.mapbox.navigation.core.telemetry.events.NavigationCancelEvent;
import com.mapbox.navigation.core.telemetry.events.NavigationDepartEvent;
import com.mapbox.navigation.core.telemetry.events.NavigationFeedbackEvent;
import com.mapbox.navigation.core.telemetry.events.NavigationRerouteEvent;
import com.mapbox.navigation.core.telemetry.events.NavigationStepData;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

public class SchemaTest {
  private static final String NAVIGATION_ARRIVE = "navigation.arrive";
  private static final String NAVIGATION_CANCEL = "navigation.cancel";
  private static final String NAVIGATION_DEPART = "navigation.depart";
  private static final String NAVIGATION_FASTER_ROUTE = "navigation.fasterRoute";
  private static final String NAVIGATION_FEEDBACK = "navigation.feedback";
  private static final String NAVIGATION_REROUTE = "navigation.reroute";
  private static ArrayList<JsonObject> schemaArray;

  @BeforeClass
  public static void downloadSchema() throws Exception {
    unpackSchemas();
  }

  @Test
  public void checkNavigationArriveEventSize() throws Exception {
    JsonObject schema = grabSchema(NAVIGATION_ARRIVE);
    List<Field> fields = grabClassFields(NavigationArriveEvent.class);

    assertEquals(schema.size(), fields.size());
  }

  @Test
  public void checkNavigationArriveEventFields() throws Exception {
    JsonObject schema = grabSchema(NAVIGATION_ARRIVE);
    List<Field> fields = grabClassFields(NavigationArriveEvent.class);

    schemaContainsFields(schema, fields);
  }

  @Test
  public void checkNavigationCancelEventSize() throws Exception {
    JsonObject schema = grabSchema(NAVIGATION_CANCEL);
    List<Field> fields = grabClassFields(NavigationCancelEvent.class);

    assertEquals(schema.size(), fields.size());
  }

  @Test
  public void checkNavigationCancelEventFields() throws Exception {
    JsonObject schema = grabSchema(NAVIGATION_CANCEL);
    List<Field> fields = grabClassFields(NavigationCancelEvent.class);

    schemaContainsFields(schema, fields);
  }

  @Test
  public void checkNavigationDepartEventSize() throws Exception {
    JsonObject schema = grabSchema(NAVIGATION_DEPART);
    List<Field> fields = grabClassFields(NavigationDepartEvent.class);

    assertEquals(schema.size(), fields.size());
  }

  @Test
  public void checkNavigationDepartEventFields() throws Exception {
    JsonObject schema = grabSchema(NAVIGATION_DEPART);
    List<Field> fields = grabClassFields(NavigationDepartEvent.class);

    schemaContainsFields(schema, fields);
  }

  @Test
  public void checkNavigationFeedbackEventSize() throws Exception {
    JsonObject schema = grabSchema(NAVIGATION_FEEDBACK);
    List<Field> fields = grabClassFields(NavigationFeedbackEvent.class);

    assertEquals(schema.size(), fields.size());
  }

  @Test
  public void checkNavigationFeedbackEventFields() throws Exception {
    JsonObject schema = grabSchema(NAVIGATION_FEEDBACK);
    List<Field> fields = grabClassFields(NavigationFeedbackEvent.class);

    schemaContainsFields(schema, fields);
  }

  @Test
  public void checkNavigationRerouteEventSize() throws Exception {
    JsonObject schema = grabSchema(NAVIGATION_REROUTE);
    List<Field> fields = grabClassFields(NavigationRerouteEvent.class);

    assertEquals(schema.size(), fields.size());
  }

  @Test
  public void checkNavigationRerouteEventFields() {
    JsonObject schema = grabSchema(NAVIGATION_REROUTE);
    List<Field> fields = grabClassFields(NavigationRerouteEvent.class);

    schemaContainsFields(schema, fields);
  }

  private void schemaContainsFields(JsonObject schema, List<Field> fields) {
    int distanceRemainingCount = 0;
    int durationRemainingCount = 0;

    for (int i = 0; i < fields.size(); i++) {
      String thisField = String.valueOf(fields.get(i));
      String[] fieldArray = thisField.split(" ");
      String[] typeArray = fieldArray[fieldArray.length - 2].split("\\.");
      String type = typeArray[typeArray.length - 1];

      String[] nameArray = fieldArray[fieldArray.length - 1].split("\\.");
      String field = nameArray[nameArray.length - 1];

      SerializedName serializedName = fields.get(i).getAnnotation(SerializedName.class);

      if (serializedName != null) {
        field = serializedName.value();
      }

      if (field.equalsIgnoreCase("durationRemaining")) {
        durationRemainingCount++;

        if (durationRemainingCount > 1) {
          field = "step" + field;
        }
      }

      if (field.equalsIgnoreCase("distanceRemaining")) {
        distanceRemainingCount++;

        if (distanceRemainingCount > 1) {
          field = "step" + field;
        }
      }

      JsonObject thisSchema = findSchema(schema, field);
      assertNotNull(field, thisSchema);

      if (thisSchema.has("type")) {
        typesMatch(thisSchema, type);
      }
    }
  }

  private JsonObject findSchema(JsonObject schema, String field) {
    JsonObject thisSchema = schema.getAsJsonObject(field);

    return thisSchema;
  }

  private void typesMatch(JsonObject schema, String type) {
    if (type.equalsIgnoreCase("int") || type.equalsIgnoreCase("integer")
        || type.equalsIgnoreCase("double") || type.equalsIgnoreCase("float")) {
      type = "number";
    }

    if (type.contains("[]")) {
      type = "array";
    }

    Class<? extends JsonElement> typeClass = schema.get("type").getClass();
    JsonElement jsonElement = new JsonParser().parse(type.toLowerCase());

    if (typeClass == JsonPrimitive.class) {
      JsonElement typePrimitive = schema.get("type");
      assertTrue(typePrimitive.equals(jsonElement));
    } else {
      JsonArray arrayOfTypes = schema.getAsJsonArray("type");
      assertTrue(arrayOfTypes.contains(jsonElement));
    }
  }

  private static ByteArrayInputStream getFileBytes() throws IOException {
    InputStream inputStream = SchemaTest.class.getClassLoader().getResourceAsStream("mobile-event-schemas.jsonl.gz");
    byte[] byteOut = IOUtils.toByteArray(inputStream);

    return new ByteArrayInputStream(byteOut);
  }

  private static void unpackSchemas() throws IOException {
    ByteArrayInputStream bais = getFileBytes();
    GZIPInputStream gzis = new GZIPInputStream(bais);
    InputStreamReader reader = new InputStreamReader(gzis);
    BufferedReader in = new BufferedReader(reader);

    schemaArray = new ArrayList<>();

    Gson gson = new Gson();
    String readed;
    while ((readed = in.readLine()) != null) {
      JsonObject schema = gson.fromJson(readed, JsonObject.class);
      schemaArray.add(schema);
    }
  }

  private JsonObject grabSchema(String eventName) {
    for (JsonObject thisSchema : schemaArray) {
      String name = thisSchema.get("name").getAsString();

      if (name.equalsIgnoreCase(eventName)) {
        Gson gson = new Gson();
        String schemaString = gson.toJson(thisSchema.get("properties"));
        JsonObject schema = gson.fromJson(thisSchema.get("properties"), JsonObject.class);

        if (schema.has("step")) {
          JsonObject stepJson = schema.get("step").getAsJsonObject();
          JsonObject stepProperties = stepJson.get("properties").getAsJsonObject();

          String stepPropertiesJson = gson.toJson(stepProperties);
          schemaString = generateStepSchemaString(stepPropertiesJson, schemaString);

          schema = gson.fromJson(schemaString, JsonObject.class);
          schema.remove("step");
        }

        schema.remove("userAgent");
        schema.remove("received");
        schema.remove("token");
        schema.remove("authorization");
        schema.remove("owner");
        schema.remove("locationAuthorization");
        schema.remove("locationEnabled");
        //temporary need to work out a solution to include this data
        schema.remove("platform");

        return schema;
      }
    }

    return null;
  }

  private List<Field> grabClassFields(Class aClass) {
    List<Field> fields = new ArrayList<>();
    Field[] allFields = aClass.getDeclaredFields();
    for (Field field : allFields) {
      if (field.getType() == NavigationStepData.class) {
        Field[] dataFields = field.getType().getDeclaredFields();
        for (Field dataField : dataFields) {
          if (Modifier.isPrivate(dataField.getModifiers()) && !Modifier.isStatic(dataField.getModifiers())) {
            fields.add(dataField);
          }
        }
      } else if (Modifier.isPrivate(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
        fields.add(field);
      }
    }
    Field[] superFields = aClass.getSuperclass().getDeclaredFields();
    for (Field field : superFields) {
      if (Modifier.isPrivate(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
        fields.add(field);
      }
    }

    return fields;
  }

  private List<Field> removeField(List<Field> fields, String fieldName) {
    for (Field field : new ArrayList<>(fields)) {
      String thisField = String.valueOf(field);
      String[] fieldArray = thisField.split("\\.");
      String simpleField = fieldArray[fieldArray.length - 1];
      if (simpleField.equalsIgnoreCase(fieldName)) {
        fields.remove(field);
      }
    }

    return fields;
  }

  private String generateStepSchemaString(String stepJson, String schemaString) {
    stepJson = stepJson.replace("\"distanceRemaining\"", "\"stepdistanceRemaining\"");
    stepJson = stepJson.replace("durationRemaining", "stepdurationRemaining");
    stepJson = stepJson.replaceFirst("\\{", ",");
    schemaString = schemaString.replaceAll("}$", "");
    schemaString = schemaString + stepJson;

    return schemaString;
  }
}
