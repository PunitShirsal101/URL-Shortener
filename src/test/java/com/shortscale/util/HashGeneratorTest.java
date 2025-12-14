package com.shortscale.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HashGeneratorTest {

    @Test
    public void shouldGenerateUniqueShortCodes() {
        HashGenerator generator = new HashGenerator();
        String code1 = generator.generateShortCode();
        String code2 = generator.generateShortCode();
        assertNotNull(code1);
        assertNotNull(code2);
        assertNotEquals(code1, code2);
        assertFalse(code1.isEmpty());
    }

    @Test
    public void shouldEncodeNumbersCorrectlyToBase62() {
        HashGenerator generator = new HashGenerator();
        // Since counter starts at 1, first call is 1 -> "1"
        assertEquals("1", generator.generateShortCode());
        assertEquals("2", generator.generateShortCode());
        assertEquals("3", generator.generateShortCode());
        // 62 -> "10" (since 62 / 62 = 1, remainder 0 -> '0', then 1 -> '1', reverse "10")
        // But counter is now 4, so need to call 59 more times? No.
        // To test encode(62), I need to set counter to 62, but can't.
        // Since private, perhaps use reflection or make it testable.
        // For coverage, perhaps call many times.
        for (int i = 4; i <= 62; i++) {
            generator.generateShortCode();
        }
        // Now counter at 63, next is 63
        // 63 / 62 = 1, remainder 1 -> '1', then 1 -> '1', reverse "11"
        String code63 = generator.generateShortCode();
        assertEquals("11", code63);
    }
}
