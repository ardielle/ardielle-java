import com.yahoo.rdl.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JSONTest {

    @Test
    public void NullTest() {
        Object o1 = null;
        String json = JSON.string(o1);
        Object o2 = JSON.fromString(json, Object.class);
        Assert.assertEquals(o1, o2);
        System.out.println("JSONTest.NullTest ok");
    }

    <T> void genericTest(T o, Class<T> cls) {
        String json = JSON.string(o);
        Object o2 = JSON.fromString(json, cls);
        Assert.assertEquals(o, o2);
    }

    @Test
    public void BoolTest() {
        genericTest(true, Boolean.class);
        genericTest(false, Boolean.class);
        System.out.println("JSONTest.BoolTest ok");
    }

    @Test
    public void NumberTest() {
        genericTest((byte)23, Byte.class);
        genericTest((short)23, Short.class);
        genericTest(23, Integer.class);
        genericTest((long)23, Long.class);
        genericTest((float)23, Float.class);
        genericTest((double)23, Double.class);
        System.out.println("JSONTest.NumberTest ok");
    }

    @Test
    public void BytesTest() {
        byte [] o1 = {1, 2, 3};
        String json = JSON.string(o1);
        byte [] o2 = JSON.fromString(json, o1.getClass());
        Assert.assertEquals(o1, o2);
        System.out.println("JSONTest.BytesTest ok");
    }

    @Test
    public void StringTest() {
        genericTest("Hello there", String.class);
        System.out.println("JSONTest.StringTest ok");
    }

    @Test
    public void TimestampTest() {
        genericTest(Timestamp.fromCurrentTime(), Timestamp.class);
        System.out.println("JSONTest.TimestampTest ok");
    }

    @Test
    public void SymbolTest() {
        Symbol bar = Symbol.intern("bar");
        Symbol foo = Symbol.intern("foo");
        Assert.assertNotEquals(bar, foo);
        genericTest(foo, Symbol.class);
        System.out.println("JSONTest.SymbolTest ok");
    }

    @Test
    public void ArrayTest() {
        List<String> lst1 = new ArrayList<String>();
        lst1.add("one");
        lst1.add("two");
        lst1.add("three");
        genericTest(lst1, List.class);
        //        String [] ary = {"one", "two", "three"};
        //        Class cls = Class.forName("byte[]");
        //        genericTest(ary, cls);
        System.out.println("JSONTest.ArrayTest ok");
    }

    @Test
    public void MapTest() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        genericTest(map, Map.class);
        System.out.println("JSONTest.MapTest ok");
    }

    @Test
    public void StructTest() {
        Struct s = new Struct().with("one", 1).with("two", 2.0).with("three", "3").with("s", new Struct().with("Hi", "there"));
        genericTest(s, Struct.class);
        System.out.println("JSONTest.StructTest ok: " + s);
    }

    //enum

    //union

    //any

    @Test
    public void UUIDTest() {
        genericTest(UUID.fromURL("http://yahoo.com"), UUID.class);
        System.out.println("JSONTest.UUIDTest ok");
    }

    //mapper to Polyline
}


