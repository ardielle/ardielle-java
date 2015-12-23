import com.yahoo.rdl.*;
import java.io.File;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

public class SchemaTest {

    Schema loadSchema(String name) {
        try {
            String path = "src/test/resources/" + name;
            byte[] jsonData = Files.readAllBytes(Paths.get(path));
            ObjectMapper objectMapper = new ObjectMapper();
            Schema schema = objectMapper.readValue(jsonData, Schema.class);
            //System.out.println("schema: " + JSON.indented(schema));
            return schema;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    Object loadData(String name) {
        try {
            String path = "src/test/resources/" + name;
            byte[] jsonData = Files.readAllBytes(Paths.get(path));
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonData, Object.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    Schema polylineSchema() {
        Schema schema = new Schema().name("tests");
        ArrayList<Type> types = new ArrayList<Type>();

        ArrayList<StructFieldDef> fields = new ArrayList<StructFieldDef>();
        fields.add(new StructFieldDef().name("x").type("Int32"));
        fields.add(new StructFieldDef().name("y").type("Int32"));
        Type point = new Type(new StructTypeDef()
                              .name("Point")
                              .type("Struct")
                              .fields(fields));
        types.add(point);

        fields = new ArrayList<StructFieldDef>();
        fields.add(new StructFieldDef().name("points").type("Array").items("Point"));
        Type polyline = new Type(new StructTypeDef()
                                 .name("Polyline")
                                 .type("Struct")
                                 .fields(fields));
        types.add(polyline);

        schema.types(types);
        return schema;
    }

    public String showSchema(Schema schema) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
            return mapper.writeValueAsString(schema);
        } catch (Exception e) {
            return "???";
        }
    }

    @Test
    public void schemaTest() {
        Validator.Result result;
        //test the schema for schema itself, as defined in RDL, and has union types in it.
        Schema schema = loadSchema("rdl_schema.json");
        Validator v = new Validator(schema);
        result = v.validate(schema, "Schema");
        Assert.assertTrue(result.valid);
        Assert.assertNull(result.error);

        Object data = loadData("bad_schema.json");
        result = v.validate(data, "Schema");
        //System.out.println("expected bad validation: " + result);
        Assert.assertFalse(result.valid);
        Assert.assertNotNull(result.error);
    }

    @Test
    public void DataTest() {
        //Schema s = loadSchema("polyline_schema.json");
        Schema schema = polylineSchema();
        System.out.println("Schema:" + showSchema(schema));
        Validator v = new Validator(schema);

        Object data = loadData("point.json");
        Validator.Result result = v.validate(data, "Point");
        Assert.assertTrue(result.valid);
        Assert.assertNull(result.error);

        data = loadData("polyline.json");
        result = v.validate(data, "Polyline");
        System.out.println("Expected positive validation: " + result);
        Assert.assertTrue(result.valid);
        Assert.assertNull(result.error);

        result = v.validate(data, "Point");
        System.out.println("expected negative validation: " + result);
        Assert.assertFalse(result.valid);
        Assert.assertNotNull(result.error);
    }

    @Test
    public void StringPatternsTest() {
        Schema s = loadSchema("basictypes_schema.json");
        Struct d1 = new Struct().with("name", "blah").with("parent", "foo");
        Struct d2 = new Struct().with("name", "blah").with("parent", "foo.bar");
        Struct d3 = new Struct().with("name", "blah").with("parent", "foo.bar.glorp");
        Struct d4 = new Struct().with("name", "blah").with("parent", ".foo");
        Struct d5 = new Struct().with("name", "blah").with("parent", "foo.");
        Struct d6 = new Struct().with("name", "blah").with("parent", "foo..bar");
        Struct d7 = new Struct().with("name", "blah").with("parent", "foo.bar").with("names", Arrays.asList(String.class, "one", "two"));
        Struct d8 = new Struct().with("name", "blah").with("parent", "foo.bar").with("names", Arrays.asList(String.class, "one", "two."));
        Struct d9 = new Struct().with("name", "blah").with("parent", "foo").with("enc", "id%3D126867%26userid%3D");
        Validator v = new Validator(s);
        Assert.assertTrue(v.validate(d1, "StringTest").valid);
        Assert.assertTrue(v.validate(d2, "StringTest").valid);
        Assert.assertTrue(v.validate(d3, "StringTest").valid);
        Assert.assertFalse(v.validate(d4, "StringTest").valid);
        Assert.assertFalse(v.validate(d5, "StringTest").valid);
        Assert.assertFalse(v.validate(d6, "StringTest").valid);
        Assert.assertTrue(v.validate(d7, "StringTest").valid);
        Assert.assertFalse(v.validate(d8, "StringTest").valid);
        Assert.assertTrue(v.validate(d9, "StringTest").valid);
    }
    /*

    @Test
    public void NameTest() {
        Schema s = loadSchema("name_types.rdl");
        Assert.assertEquals(s.validate("foo", "SimpleName").get("valid"), true);
        Assert.assertNotNull(s.validate("foo.bar", "SimpleName").get("error"));
        Assert.assertEquals(s.validate("foo:", "YRN").get("valid"), true);
        Assert.assertEquals(s.validate("foo.com:", "YRN").get("valid"), true);
        Assert.assertEquals(s.validate("foo.com:blah", "YRN").get("valid"), true);
        Assert.assertEquals(s.validate("foo.com:blah.glorp", "YRN").get("valid"), true);
        System.out.println("done");
    }
    */

    @Test
    public void UUIDTest() {
        Schema schema = loadSchema("basictypes_schema.json");
        Validator v = new Validator(schema);
        Object data = loadData("uuid.json");
        Validator.Result result = v.validate(data, "UUIDTest");
        Assert.assertTrue(result.valid);
    }

    /*


    @Test
    public void MapTest() {
        Schema s = loadSchema("basictypes.rdl");
        Struct data = loadData("map.json");
        Struct result = s.validate(data, "MapTest");
        Assert.assertNotNull(result.get("error"));
    }

    @Test
    public void IntegerOutOfBoundTest() {
        Schema s = loadSchema("basictypes.rdl");
        Struct data = loadData("intoutofbound.json");
        Struct result = s.validate(data, "IntOOBTest");
        Assert.assertNotNull(result.get("error"));
    }

    @Test
    public void NegativeNumberTest() {
        Schema s = loadSchema("basictypes.rdl");
        Struct data = loadData("negative.json");
        Struct result = s.validate(data, "NegativeNumberTest");
        Assert.assertEquals(result.get("valid"), true);
    }

    @Test
    public void RequiredItemTest() {
        Schema s = loadSchema("required_example.rdl");
        Struct data = loadData("required.json");
        Struct result = s.validate(data, "Types_that_required");
        Assert.assertEquals(result.get("valid"), true);
    }

    @Test
    public void RequiredClosedItemTest() {
        Schema s = loadSchema("required_example.rdl");
        Struct data = loadData("required_closed.json");
        Struct result = s.validate(data, "Types_that_required_with_closed");
        Assert.assertEquals(result.get("valid"), true);
    }


    @Test
    public void WithoutRequiredItemTest() {
        Schema s = loadSchema("required_example.rdl");
        Struct data = loadData("without_required.json");
        Struct result = s.validate(data, "Types_that_required");
        Assert.assertNotNull(result.get("error"));
    }

    @Test
    public void RequiredRegexpItemTest() {
        Schema s = loadSchema("required_example.rdl");
        Struct data = loadData("without_required_regexp.json");
        Struct result = s.validate(data, "Types_that_required");
        Assert.assertNotNull(result.get("error"));
    }

    @Test
    public void InheritedTypeTest() {
        Schema s = loadSchema("closed_type.rdl");
        Struct data = loadData("inherited_type.json");
        Struct result = s.validate(data, "OpenMovieFacet");
        Assert.assertNotNull(result.get("error"));
    }

    @Test
    public void ClosedTypeTest() {
        Schema s = loadSchema("closed_type.rdl");
        Struct data = loadData("closed_type.json");
        Struct result = s.validate(data, "MovieFacet");
        Assert.assertNotNull(result.get("error"));
    }

    @Test ( enabled = true )
    public void RegexpStringTest() {
        Schema s = loadSchema("regexp_string.rdl");
        Struct data = loadData("regexp_string.json");
        Struct result = s.validate(data, "Test");
        Assert.assertEquals(result.get("valid"),true , "testing regexp string , result is :" + result );

        data = loadData("regexp2_string.json");
        result = s.validate(data, "NetworkLocation");
        Assert.assertEquals(result.get("valid"),true , "testing regex IPAddress type, result is :" + result );
    }

    @Test ( enabled = false )
    public void SpecialEnumTest() {
        Schema s = loadSchema("special_enum.rdl");
        Struct data = loadData("special_enum.json");
        Struct result = s.validate(data, "Test");
        Assert.assertEquals(result.get("valid"),true , "testing special enum , result is :" + result );
    }

    @Test ( enabled = true )
    public void SpecialEnumAlternativeTest() {
        Schema s = loadSchema("special_enum_alt.rdl");
        Struct data = loadData("special_enum.json");
        Struct result = s.validate(data, "Test");
        Assert.assertEquals(result.get("valid"),true , "testing special enum alternative, result is :" + result );
    }

    @Test ( enabled = false )
    public void UTFEnumTest() {
        Schema s = loadSchema("special_enum.rdl");
        Struct data = loadData("utf_enum.json");
        Struct result = s.validate(data, "UTFOptionsTest");
        Assert.assertEquals(result.get("valid"),true , "testing utf8 enum , result is :" + result );
    }
    */
}
