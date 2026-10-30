package com.webank.ai.fate.serving;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.IntegerTypeHandler;
import org.junit.Test;

import static org.junit.Assert.*;

/** Local compatibility checks. Child JVMs use only synthetic serializable values. */
public class MyBatisSecurityTest {
    @Test
    public void loadedArtifactIsThePatchedCandidate() throws Exception {
        File jar = new File(Configuration.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (JarFile archive = new JarFile(jar)) {
            Properties properties = new Properties();
            try (InputStream stream = archive.getInputStream(archive.getJarEntry(
                    "META-INF/maven/org.mybatis/mybatis/pom.properties"))) {
                properties.load(stream);
            }
            assertEquals("3.5.6", properties.getProperty("version"));
        }
    }

    @Test
    public void ordinarySerializedCacheCopiesValues() {
        SerializedCache cache = new SerializedCache(new PerpetualCache("local-roundtrip"));
        List<String> source = new ArrayList<String>();
        source.add("synthetic value");
        cache.putObject("key", source);
        assertEquals(source, cache.getObject("key"));
        assertNotSame(source, cache.getObject("key"));
    }

    @Test
    public void mapperAnnotationAndBoundParameterRemainCompatible() {
        Configuration configuration = new Configuration();
        configuration.addMapper(LocalMapper.class);
        BoundSql sql = configuration.getMappedStatement(LocalMapper.class.getName() + ".label")
                .getBoundSql(Collections.singletonMap("name", "synthetic"));
        assertEquals("select ? as label", sql.getSql());
        assertEquals(1, sql.getParameterMappings().size());
        assertEquals("name", sql.getParameterMappings().get(0).getProperty());
    }

    @Test
    public void reflectionAndTypeHandlerDefaultsRemainCompatible() {
        Configuration configuration = new Configuration();
        Payload payload = new Payload();
        configuration.newMetaObject(payload).setValue("value", 42);
        assertEquals(42, payload.getValue());
        assertTrue(configuration.getTypeHandlerRegistry().getTypeHandler(Integer.class)
                instanceof IntegerTypeHandler);
    }

    @Test
    public void missingSerialFilterProducesOneWarningInFreshJvm() throws Exception {
        runChild("warning", null);
    }

    @Test
    public void configuredSerialFilterRejectsOnlySyntheticDeniedClass() throws Exception {
        runChild("filter", "!" + Payload.class.getName() + ";*");
    }

    private static void runChild(String mode, String filter) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(new File(System.getProperty("java.home"), "bin/java").getPath());
        command.add("-Xmx64m");
        if (filter != null) command.add("-Djdk.serialFilter=" + filter);
        command.add("-cp");
        StringBuilder classPath = new StringBuilder();
        Class<?>[] inputs = {MyBatisSecurityTest.class, Configuration.class,
                org.junit.Assert.class, org.hamcrest.Matcher.class};
        for (Class<?> input : inputs) {
            if (classPath.length() > 0) classPath.append(File.pathSeparator);
            classPath.append(new File(input.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
        }
        command.add(classPath.toString());
        command.add(MyBatisSecurityTest.class.getName());
        command.add(mode);
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().remove("JAVA_TOOL_OPTIONS");
        builder.environment().remove("_JAVA_OPTIONS");
        builder.environment().remove("JDK_JAVA_OPTIONS");
        Process process = builder.start();
        try {
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("bounded synthetic child JVM timed out");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            try (InputStream input = process.getInputStream()) {
                int count;
                while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
            }
            assertEquals(output.toString("UTF-8"), 0, process.exitValue());
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    public static void main(String[] args) {
        final int[] warnings = {0};
        LogFactory.useJdkLogging();
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) root.removeHandler(handler);
        root.addHandler(new Handler() {
            public void publish(LogRecord record) {
                if (record.getMessage().contains("JEP-290 serial filter")) warnings[0]++;
            }
            public void flush() { }
            public void close() { }
        });
        SerializedCache cache = new SerializedCache(new PerpetualCache("synthetic-child"));
        ArrayList<String> source = new ArrayList<String>();
        source.add("allowed synthetic value");
        cache.putObject("list", source);
        assertEquals(source, cache.getObject("list"));
        cache.getObject("list");
        if ("warning".equals(args[0])) {
            assertEquals("exactly one missing-filter warning", 1, warnings[0]);
        } else {
            cache.putObject("payload", new Payload());
            boolean rejected = false;
            try { cache.getObject("payload"); }
            catch (CacheException exception) {
                rejected = exception.toString().contains("filter status: REJECTED");
            }
            assertTrue("configured JEP-290 filter must reject the synthetic class", rejected);
            assertEquals("configured filter must not warn", 0, warnings[0]);
        }
        System.out.println("PASS " + args[0]);
    }

    public interface LocalMapper {
        @Select("select #{name} as label")
        String label(@Param("name") String name);
    }
    public static class Payload implements Serializable {
        private static final long serialVersionUID = 1L;
        private int value = 37;
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}
