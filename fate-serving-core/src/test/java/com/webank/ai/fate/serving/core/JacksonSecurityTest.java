package com.webank.ai.fate.serving.core;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Bounded, local-only regressions for the real Jackson Dependabot alerts. */
public class JacksonSecurityTest {
    public static class NeverInstantiate {
        static int constructions;
        public NeverInstantiate() { ++constructions; }
        public String value;
    }
    public static class TypedValue {
        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_ARRAY)
        public Object value;
    }
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="kind", defaultImpl=Fallback.class)
    @JsonSubTypes(@JsonSubTypes.Type(value=Known.class, name="known"))
    public static class PolymorphicValue { public int value; }
    public static class Fallback extends PolymorphicValue { }
    public static class Known extends PolymorphicValue { }

    @Test public void componentsUseTheFixedRelease() {
        assertEquals("2.18.11", new ObjectMapper().version().toString());
        assertEquals("2.18.11", new JsonFactory().version().toString());
        assertEquals("2.18.11", JsonProperty.class.getPackage().getImplementationVersion());
    }
    @Test public void deniedGenericElementIsNotInstantiated() throws Exception {
        NeverInstantiate.constructions=0;
        ObjectMapper m=new ObjectMapper();
        m.setPolymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("java.util.ArrayList").build());
        String type="java.util.ArrayList<"+NeverInstantiate.class.getName()+">";
        try { m.readValue("{\"value\":[\""+type+"\",[{}]]}",TypedValue.class);
              fail("Denied generic component must fail closed"); }
        catch (InvalidTypeIdException expected) { }
        assertEquals(0,NeverInstantiate.constructions);
    }
    @Test public void deniedArrayElementIsNotInstantiated() throws Exception {
        NeverInstantiate.constructions=0;
        ObjectMapper m=new ObjectMapper();
        m.setPolymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
            .allowIfSubTypeIsArray().allowIfSubType("java.lang.String").build());
        String type="[L"+NeverInstantiate.class.getName()+";";
        try { m.readValue("{\"value\":[\""+type+"\",[{}]]}",TypedValue.class);
              fail("Denied array component must fail closed"); }
        catch (InvalidTypeIdException expected) { }
        assertEquals(0,NeverInstantiate.constructions);
        TypedValue allowed=m.readValue("{\"value\":[\"[Ljava.lang.String;\",[\"ok\"]]}",TypedValue.class);
        assertArrayEquals(new String[]{"ok"},(String[])allowed.value);
    }
    private static Map<?,?> subtypeCache(ObjectMapper m) throws Exception {
        Field roots=ObjectMapper.class.getDeclaredField("_rootDeserializers"); roots.setAccessible(true);
        Object root=((Map<?,?>)roots.get(m)).get(m.constructType(PolymorphicValue.class));
        Field type=root.getClass().getDeclaredField("_typeDeserializer"); type.setAccessible(true);
        Object td=type.get(root); Class<?> c=td.getClass();
        while(c!=null) {
            try { Field cache=c.getDeclaredField("_deserializers");cache.setAccessible(true);
                  return (Map<?,?>)cache.get(td); }
            catch(NoSuchFieldException e) { c=c.getSuperclass(); }
        }
        throw new AssertionError("Jackson subtype cache not found");
    }
    @Test public void distinctUnknownTypeIdsHaveBoundedFallbackCache() throws Exception {
        ObjectMapper distinct=new ObjectMapper(), repeated=new ObjectMapper();
        for(int i=0;i<2500;i++) {
            PolymorphicValue a=distinct.readValue("{\"kind\":\"unknown-"+i+"\",\"value\":7}",PolymorphicValue.class);
            assertTrue(a instanceof Fallback); assertEquals(7,a.value);
            repeated.readValue("{\"kind\":\"unknown\"}",PolymorphicValue.class);
            assertTrue(subtypeCache(distinct).size()<=1000);
        }
        assertTrue("Upstream fixed cache must stay within its 1000-entry bound",subtypeCache(distinct).size()<=1000);
        assertTrue(subtypeCache(repeated).size()<=1);
        assertTrue(distinct.readValue("{\"kind\":\"known\",\"value\":9}",PolymorphicValue.class) instanceof Known);
    }
    public static class CountingId {
        static long comparisons; final String value;
        @JsonCreator public CountingId(String value){this.value=value;}
        @JsonValue public String value(){return value;}
        @Override public int hashCode(){return value.hashCode();}
        @Override public boolean equals(Object o){++comparisons;return o instanceof CountingId && value.equals(((CountingId)o).value);}
    }
    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class,property="id")
    public static class IdentityValue { public CountingId id; public int value; }
    @Test public void reverseIdentityResolutionHasLinearWorkAndPreservesOrder() throws Exception {
        final int n=400; StringBuilder json=new StringBuilder("[");
        for(int i=0;i<n;i++) {if(i>0)json.append(',');json.append('"').append("id").append(i).append('"');}
        for(int i=n-1;i>=0;i--)json.append(",{\"id\":\"id").append(i).append("\",\"value\":").append(i).append('}');
        json.append(']'); CountingId.comparisons=0;
        ObjectMapper m=new ObjectMapper();
        List<IdentityValue> values=m.readValue(json.toString(),m.getTypeFactory().constructCollectionType(List.class,IdentityValue.class));
        assertEquals(2*n,values.size());
        for(int i=0;i<n;i++){assertEquals(i,values.get(i).value);assertSame(values.get(i),values.get(2*n-1-i));}
        assertTrue("Identity lookup comparisons must be linear, got "+CountingId.comparisons,CountingId.comparisons<20L*n);
    }
    @Test public void unresolvedAndDuplicateIdentityReferencesFailClosed() throws Exception {
        ObjectMapper m=new ObjectMapper(); JavaType list=m.getTypeFactory().constructCollectionType(List.class,IdentityValue.class);
        for(String json:new String[]{"[\"missing\"]","[{\"id\":\"same\"},{\"id\":\"same\"}]"}) {
            try {m.readValue(json,list);fail("Invalid identity graph must fail closed");}
            catch(JsonMappingException expected) { }
        }
    }
}
