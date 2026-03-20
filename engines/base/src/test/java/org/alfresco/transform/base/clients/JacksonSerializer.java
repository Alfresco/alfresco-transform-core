/*
 * Copyright 2015-2022 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.transform.base.clients;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import com.fasterxml.jackson.annotation.JsonInclude.Include;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JacksonSerializer
{
    private static final ObjectMapper MAPPER;

    static
    {
        MAPPER = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(Include.NON_NULL))
                .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(Include.NON_NULL))
                .build();
    }

    public static <T> byte[] serialize(T value) throws Exception
    {
        try (final ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
                final OutputStreamWriter writer = new OutputStreamWriter(stream, UTF_8))
        {
            MAPPER.writer().writeValue(writer, value);
            return stream.toByteArray();
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> cls) throws Exception
    {
        return MAPPER.readValue(data, cls);
    }

    public static <T> T deserialize(byte[] data, int len, Class<T> cls) throws Exception
    {
        return MAPPER.readValue(data, 0, len, cls);
    }

    public static String readStringValue(String json, String key) throws Exception
    {
        JsonNode node = MAPPER.readTree(json);
        for (String k : key.split("\\."))
        {
            node = node.get(k);
        }
        return node.asText();
    }
}
