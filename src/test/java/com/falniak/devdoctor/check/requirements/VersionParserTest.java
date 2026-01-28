package com.falniak.devdoctor.check.requirements;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionParserTest {

    @Test
    void testParseNodeVersion() {
        // Exact versions
        assertEquals(18, VersionParser.parseNodeVersion("18"));
        assertEquals(18, VersionParser.parseNodeVersion("v18.19.0"));
        assertEquals(20, VersionParser.parseNodeVersion("20.10.5"));
        
        // Range versions
        assertEquals(20, VersionParser.parseNodeVersion("^20"));
        assertEquals(20, VersionParser.parseNodeVersion("20.x"));
        assertEquals(18, VersionParser.parseNodeVersion(">=18"));
        assertEquals(18, VersionParser.parseNodeVersion(">=18.0.0"));
        
        // Edge cases
        assertNull(VersionParser.parseNodeVersion(null));
        assertNull(VersionParser.parseNodeVersion(""));
        assertNull(VersionParser.parseNodeVersion("invalid"));
        assertNull(VersionParser.parseNodeVersion("abc"));
    }

    @Test
    void testParsePythonVersion() {
        // Exact versions
        assertEquals(3, VersionParser.parsePythonVersion("3.11"));
        assertEquals(3, VersionParser.parsePythonVersion("3.9"));
        
        // Range versions
        assertEquals(3, VersionParser.parsePythonVersion(">=3.9"));
        assertEquals(3, VersionParser.parsePythonVersion(">=3.11.0"));
        
        // Edge cases
        assertNull(VersionParser.parsePythonVersion(null));
        assertNull(VersionParser.parsePythonVersion(""));
        assertNull(VersionParser.parsePythonVersion("invalid"));
        assertNull(VersionParser.parsePythonVersion("3"));
    }

    @Test
    void testParseGoVersion() {
        // Standard format
        assertEquals(21, VersionParser.parseGoVersion("go 1.21"));
        assertEquals(20, VersionParser.parseGoVersion("go 1.20"));
        assertEquals(19, VersionParser.parseGoVersion("go 1.19"));
        
        // Edge cases
        assertNull(VersionParser.parseGoVersion(null));
        assertNull(VersionParser.parseGoVersion(""));
        assertNull(VersionParser.parseGoVersion("invalid"));
        assertNull(VersionParser.parseGoVersion("1.21"));
        assertNull(VersionParser.parseGoVersion("go1.21"));
    }

    @Test
    void testParseJavaVersion() {
        // Modern format
        assertEquals(17, VersionParser.parseJavaVersion("17"));
        assertEquals(21, VersionParser.parseJavaVersion("21"));
        
        // Legacy format
        assertEquals(17, VersionParser.parseJavaVersion("1.17"));
        assertEquals(8, VersionParser.parseJavaVersion("1.8"));
        
        // Modern format with patch versions
        assertEquals(17, VersionParser.parseJavaVersion("17.0.1"));
        assertEquals(21, VersionParser.parseJavaVersion("21.0.2"));
        
        // Edge cases
        assertNull(VersionParser.parseJavaVersion(null));
        assertNull(VersionParser.parseJavaVersion(""));
        assertNull(VersionParser.parseJavaVersion("invalid"));
    }

    @Test
    void testSatisfiesRequirement() {
        // Node versions - exact match
        assertTrue(VersionParser.satisfiesRequirement("18", "v18.19.0", VersionParser::parseNodeVersion));
        assertTrue(VersionParser.satisfiesRequirement("20", "20.10.5", VersionParser::parseNodeVersion));
        
        // Node versions - range match
        assertTrue(VersionParser.satisfiesRequirement(">=18", "v18.19.0", VersionParser::parseNodeVersion));
        assertTrue(VersionParser.satisfiesRequirement(">=18", "v20.0.0", VersionParser::parseNodeVersion));
        assertFalse(VersionParser.satisfiesRequirement(">=20", "v18.19.0", VersionParser::parseNodeVersion));
        
        // Python versions - Note: satisfiesRequirement does major-only comparison
        // PythonRequirementCheck uses parsePythonVersionMinor for accurate major.minor comparison
        // These tests verify the utility method works for major-only (used as fallback)
        assertTrue(VersionParser.satisfiesRequirement(">=3.9", "3.11.0", VersionParser::parsePythonVersion));
        // Note: ">=3.11" vs "3.9.0" would return true with major-only (both major 3),
        // but PythonRequirementCheck uses parsePythonVersionMinor which correctly returns false
        
        // Go versions
        assertTrue(VersionParser.satisfiesRequirement("go 1.21", "go 1.21.0", VersionParser::parseGoVersion));
        assertFalse(VersionParser.satisfiesRequirement("go 1.21", "go 1.20.0", VersionParser::parseGoVersion));
        
        // Java versions
        assertTrue(VersionParser.satisfiesRequirement("17", "17.0.1", VersionParser::parseJavaVersion));
        assertTrue(VersionParser.satisfiesRequirement("1.17", "17.0.1", VersionParser::parseJavaVersion));
        assertFalse(VersionParser.satisfiesRequirement("17", "21.0.1", VersionParser::parseJavaVersion));
        
        // Null cases
        assertFalse(VersionParser.satisfiesRequirement(null, "18", VersionParser::parseNodeVersion));
        assertFalse(VersionParser.satisfiesRequirement("18", null, VersionParser::parseNodeVersion));
        assertFalse(VersionParser.satisfiesRequirement("invalid", "18", VersionParser::parseNodeVersion));
        assertFalse(VersionParser.satisfiesRequirement("18", "invalid", VersionParser::parseNodeVersion));
    }
}
