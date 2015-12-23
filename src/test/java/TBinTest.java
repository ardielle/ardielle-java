import com.yahoo.rdl.*;
import com.yahoo.tbin.*;
import java.io.File;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
public class TBinTest {
    
    public static void main(String [] args) throws Exception {
        //new TBinTest().benchmark();
        new TBinTest().selfTest();
    }

    void genericTest(Object o1) {
        byte [] tbin = TBin.bytes(o1);
        Object o2 = TBin.fromBytes(tbin);
        assertTrue(equivalent(o1, o2));
    }
    
    //@Test
    public void selfTest() throws IOException {
        byte [] tbinSchema = Files.readAllBytes(Paths.get("src/test/resources/rdl_schema.tbin"));
        Schema schema = JSON.fromBytes(Files.readAllBytes(Paths.get("src/test/resources/rdl_schema.json")), Schema.class);
        Schema schema2 = TBin.fromBytes(tbinSchema, Schema.class);
        assertEquals(schema, schema2);

        byte [] tbin = TBin.bytes(schema);
        Schema schema3 = TBin.fromBytes(tbin, Schema.class);
        assertEquals(schema, schema3);
    }

    static Object polylineAsPOJO() throws IOException {
        return JSON.fromBytes(Files.readAllBytes(Paths.get("src/test/resources/polyline.json")), Polyline.class);
    }

    static Object polylineAsStruct() throws IOException {
        byte [] b = TBin.bytes(polylineAsPOJO());
        return TBin.fromBytes(b); //tbin generic decode maps objects onto Structs, not Maps
    }
    static Object polylineAsMap() throws IOException {
        return JSON.fromBytes(Files.readAllBytes(Paths.get("src/test/resources/polyline.json")), Object.class);
    }

    @Test
    public void testMapperEncodeDecode() {
        try {
            Polyline o1 = JSON.fromBytes(Files.readAllBytes(Paths.get("src/test/resources/polyline.json")), Polyline.class);
            System.out.println("Object: " + JSON.string(o1));
            byte [] tbin = TBin.bytes(o1);
            System.out.println("Encoded to " + tbin.length + " bytes:");
            System.out.println(hex(tbin));
            Object o2 = TBin.fromBytes(tbin, Polyline.class);
            System.out.println("decoded to: " + JSON.string(o2));
            assertEquals(o1, o2);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception");
        }
    }
    
    @Test
    public void testGenericEncodeDecode() {
        try {
            Object o = JSON.fromBytes(Files.readAllBytes(Paths.get("src/test/resources/polyline.json")), Object.class);
            genericTest(o);
        } catch (IOException e) {
            fail("Unexpected exception");
        }
    }
    
    @Test
    public void testOptimizedDecode() {
        try {
            Object o1 = JSON.fromBytes(Files.readAllBytes(Paths.get("src/test/resources/polyline.json")), Object.class);
            Object o2 = TBin.fromBytes(Files.readAllBytes(Paths.get("src/test/resources/test.tbin")));
            assertTrue(equivalent(o1, o2));
        } catch (IOException e) {
            fail("Unexpected exception");
        }
    }

    byte [] benchmarkJSONEncode(Object data, int iterations, Class<?> type) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        long t0, t1, dt;
        byte [] bref = JSON.bytes(data);
        byte [] b;
        t0 = System.currentTimeMillis();
        for (int i=0; i<iterations; i++) {
            b = JSON.bytes(data);
        }
        t1 = System.currentTimeMillis();
        dt = (t1 - t0);
        System.out.println("JSONEncode (" + data.getClass().getName() + "): " + bref.length + " bytes: " + (dt * 1000.0 / iterations) + " μs/iteration");
        return bref;
    }

    void benchmarkJSONDecode(byte [] data, int iterations, Class<?> type) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        long t0, t1, dt;
        Object o = null;
        t0 = System.currentTimeMillis();
        for (int i=0; i<iterations; i++) {
            o = JSON.fromBytes(data, type);
        }
        t1 = System.currentTimeMillis();
        dt = (t1 - t0);
        System.out.println("JSONDecode (" + type.getName() + "): " + data.length + " bytes: " + (dt * 1000.0 / iterations) + " μs/iteration");
        //System.out.println(o.getClass().getName() + " -> " + JSON.string(o));
    }

    byte [] benchmarkTBinEncode(Object data, int iterations, Class<?> type) throws IOException {
        long t0, t1, dt;
        byte [] bref = TBin.bytes(data);
        t0 = System.currentTimeMillis();
        byte [] b;
        for (int i=0; i<iterations; i++) {
            b = TBin.bytes(data);
        }
        t1 = System.currentTimeMillis();
        dt = (t1 - t0);
        System.out.println("TBinEncode (" + data.getClass().getName() + "): " + bref.length + " bytes: " + (dt * 1000.0 / iterations) + " μs/iteration");
        return bref;
    }

    void benchmarkTBinDecode(byte [] data, int iterations, Class<?> type) throws IOException {
        long t0, t1, dt;
        Object d = null;
        t0 = System.currentTimeMillis();
        for (int i=0; i<iterations; i++) {
            d = TBin.fromBytes(data, type);
        }
        t1 = System.currentTimeMillis();
        dt = (t1 - t0);
        System.out.println("TBinDecode (" + type.getName() + "): " + data.length + " bytes: " + (dt * 1000.0 / iterations) + " μs/iteration");
        //System.out.println("-> " + JSON.string(d));
    }

    public void benchmark() {
        try {
            //int iterations = 1;
            int iterations = 1000000;

            byte [] b1 = benchmarkTBinEncode(polylineAsMap(), iterations, Object.class);
            byte [] b2 = benchmarkTBinEncode(polylineAsStruct(), iterations, Object.class);
            byte [] b3 = benchmarkTBinEncode(polylineAsPOJO(), iterations, Polyline.class);
            benchmarkTBinDecode(b1, iterations, Object.class);
            benchmarkTBinDecode(b2, iterations, Object.class);
            benchmarkTBinDecode(b3, iterations, Object.class);
            benchmarkTBinDecode(b1, iterations, Polyline.class);
            benchmarkTBinDecode(b2, iterations, Polyline.class);
            benchmarkTBinDecode(b3, iterations, Polyline.class);

            byte [] b4 = benchmarkJSONEncode(polylineAsMap(), iterations, Object.class);
            byte [] b5 = benchmarkJSONEncode(polylineAsStruct(), iterations, Object.class);
            byte [] b6 = benchmarkJSONEncode(polylineAsPOJO(), iterations, Polyline.class);
            //all three should be identical
            benchmarkJSONDecode(b4, iterations, Object.class);
            benchmarkJSONDecode(b4, iterations, Struct.class);
            benchmarkJSONDecode(b4, iterations, Polyline.class);


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("*** " + e.getMessage());
            System.exit(1);
        }
    }

    //equivalent: ignores order in Maps, sinces keys can move around when going through JSON (Jackson is HashMap
    //based). Note that Structs implement Map<String,Object>, so they get included in that, allowing a
    //Map and a Struct to be equivalent.
    boolean equivalent(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1 instanceof Map && o2 instanceof Map) {
            Map m1 = (Map)o1;
            Map m2 = (Map)o2;
            if (m1.size() != m2.size()) {
                return false;
            }
            for (Object k : m1.keySet()) {
                Object v1 = m1.get(k);
                if (!m2.containsKey(k)) {
                    return false;
                }
                Object v2 = m2.get(k);
                if (!equivalent(v1, v2)) {
                    return false;
                }
            }
            return true;
        }
        if (o1 instanceof List && o2 instanceof List) {
            List l1 = (List)o1;
            List l2 = (List)o2;
            int size1 = l1.size();
            if (size1 != l2.size()) {
                return false;
            }
            for (int i=0; i<size1; i++) {
                if (!equivalent(l1.get(i), l2.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return o1.equals(o2);
    }

    final private static char[] hexArray = "0123456789abcdef".toCharArray();
    public static String hex(byte [] bytes) {
        return hex(bytes, bytes.length);
    }
    public static String hex(byte [] bytes, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<count; i++) {
            byte b = bytes[i];
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(hexArray[(b >>> 4) & 15]);
            sb.append(hexArray[b & 15]);
        }
        return sb.toString();
    }

    /*
    void writePoint(TBinEncoder enc, int x, int y) throws IOException {
        enc.writeStruct(2);
        enc.writeField("x");
        enc.writeInt(x);
        enc.writeField("y");
        enc.writeInt(y);
    }

    void writeTestLine(TBinEncoder enc) {
        //creates the polyline data
        //{"points":[{"x":1,"y":11},{"x":2,"y":22},{"x":3,"y":33},{"x":10,"y":100},{"x":-23,"y":100},{"x":-23,"y":-33},{"x":10,"y":-33},{"x":103,"y":333},{"x":300,"y":1000},{"x":1234,"y":1234},{"x":12345678,"y":12321312},{"x":321321321,"y":33},{"x":1,"y":11}]}
        //this syntax is not great, but is simple (no hidden state).
        gen.writeStruct(1); //1 field
        gen.writeField("points"); //name of the field
        gen.writeArray(13); //an array of 13 elements

        writePoint(gen, 1, 11); //write each item
        writePoint(gen, 2, 22);
        writePoint(gen, 3, 33);
        writePoint(gen, 10, 100);
        writePoint(gen, -23, 100);
        writePoint(gen, -23, -33);
        writePoint(gen, 10, -33);
        writePoint(gen, 103, 333);
        writePoint(gen, 300, 1000);
        writePoint(gen, 1234, 1234);
        writePoint(gen, 12345678, 12321312);
        writePoint(gen, 321321321, 33);
        writePoint(gen, 1, 11);
    }
    @Test
    public void testSimpleDecode() {
        //creates the polyline data
        //{"points":[{"x":1,"y":11},{"x":2,"y":22},{"x":3,"y":33},{"x":10,"y":100},{"x":-23,"y":100},{"x":-23,"y":-33},{"x":10,"y":-33},{"x":103,"y":333},{"x":300,"y":1000},{"x":1234,"y":1234},{"x":12345678,"y":12321312},{"x":321321321,"y":33},{"x":1,"y":11}]}
        //this syntax is not great, but is simple (no hidden state).
        File f = new File("src/test/resources/polyline_test.tbin_v3");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        TBinEncoder enc = new TBinEncoder(buf);
        enc.close();
        byte [] testBytes = buf.toBytes();
        byte [] refBytes = fileBytes(new File("src/test/resources/test_generic.tbin"));

        String s1 = fileContents(f);
        String s2 = fileContents(new File("src/test/data/polyline.tbin_v3"));
        assertEquals(s1, s2);
        System.out.println("The generated tbin_v3 (no schema) file matches the reference");
    }

*/


}

