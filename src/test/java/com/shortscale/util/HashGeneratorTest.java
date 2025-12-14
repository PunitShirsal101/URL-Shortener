package com.shortscale.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HashGeneratorTest {

    @Test
    public void testGenerateShortCode() {
        HashGenerator generator = new HashGenerator();
        String code1 = generator.generateShortCode();
        String code2 = generator.generateShortCode();
        assertNotNull(code1);
        assertNotNull(code2);
        assertNotEquals(code1, code2);
        assertFalse(code1.isEmpty());
    }
}
