package com.webank.ai.fate.serving.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.XmlStreamReader;
import org.junit.Test;

import static org.junit.Assert.*;

/** Bounded, local API/encoding regressions; no XML parser or external service is started. */
public class CommonsIoSecurityTest {
    @Test
    public void encodingPatternAcceptsValidXmlDeclaration() {
        assertTrue(XmlStreamReader.ENCODING_PATTERN
                .matcher("<?xml version='1.0' encoding='UTF-8'?>").find());
    }

    @Test
    public void encodingPatternDoesNotSearchArbitraryPrefix() {
        assertFalse(XmlStreamReader.ENCODING_PATTERN
                .matcher("not-an-xml-prolog <?xml version='1.0' encoding='UTF-8'?>").find());
    }

    @Test
    public void encodingPatternRequiresVersionBeforeEncoding() {
        assertFalse(XmlStreamReader.ENCODING_PATTERN
                .matcher("<?xml encoding='UTF-8'?>").find());
    }

    @Test(timeout = 2000)
    public void boundedMalformedPrologDoesNotMatch() {
        StringBuilder prolog = new StringBuilder("<?xml ");
        for (int i = 0; i < 1024; i++) prolog.append(' ');
        prolog.append("encoding='UTF-8'?>");
        assertFalse(XmlStreamReader.ENCODING_PATTERN.matcher(prolog).find());
    }

    @Test
    public void readsUtf8XmlWithoutChangingText() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?><model>模型</model>";
        try (XmlStreamReader reader = new XmlStreamReader(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))) {
            assertEquals("UTF-8", reader.getEncoding());
            assertEquals(xml, IOUtils.toString(reader));
        }
    }

    @Test
    public void readsUtf16XmlWithBom() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-16'?><model>模型</model>";
        try (XmlStreamReader reader = new XmlStreamReader(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_16)))) {
            assertEquals("UTF-16BE", reader.getEncoding());
            assertEquals(xml, IOUtils.toString(reader));
        }
    }

    @Test
    public void copyPreservesBinaryContent() throws Exception {
        byte[] input = new byte[16385];
        for (int i = 0; i < input.length; i++) input[i] = (byte) i;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertEquals(input.length, IOUtils.copy(new ByteArrayInputStream(input), output));
        assertArrayEquals(input, output.toByteArray());
    }

    @Test
    public void closeQuietlyClosesAndSuppressesIoFailure() {
        final int[] calls = {0};
        IOUtils.closeQuietly(new Closeable() {
            public void close() throws IOException {
                calls[0]++;
                throw new IOException("synthetic close failure");
            }
        });
        IOUtils.closeQuietly((Closeable) null);
        assertEquals(1, calls[0]);
    }
}
