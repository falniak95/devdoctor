package com.falniak.devdoctor.check.requirements;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads Java version requirements from pom.xml and build.gradle files.
 */
public class JavaRequirementReader {

    private static final Pattern GRADLE_SOURCE_COMPATIBILITY_PATTERN = Pattern.compile(
        "sourceCompatibility\\s*=\\s*(?:JavaVersion\\.VERSION_(\\d+)|[\"'](\\d+)[\"'])"
    );

    /**
     * Reads the Java version requirement from the project root.
     *
     * @param projectRoot The project root directory
     * @return Optional Requirement if found, empty otherwise
     */
    public Optional<Requirement> read(Path projectRoot) {
        // Try Maven pom.xml first
        Path pomXmlPath = projectRoot.resolve("pom.xml");
        if (Files.exists(pomXmlPath) && Files.isRegularFile(pomXmlPath)) {
            Optional<Requirement> mavenReq = readFromPomXml(pomXmlPath);
            if (mavenReq.isPresent()) {
                return mavenReq;
            }
        }

        // Try Gradle build.gradle
        Path buildGradlePath = projectRoot.resolve("build.gradle");
        if (Files.exists(buildGradlePath) && Files.isRegularFile(buildGradlePath)) {
            Optional<Requirement> gradleReq = readFromBuildGradle(buildGradlePath);
            if (gradleReq.isPresent()) {
                return gradleReq;
            }
        }

        // Try Gradle build.gradle.kts
        Path buildGradleKtsPath = projectRoot.resolve("build.gradle.kts");
        if (Files.exists(buildGradleKtsPath) && Files.isRegularFile(buildGradleKtsPath)) {
            Optional<Requirement> gradleKtsReq = readFromBuildGradle(buildGradleKtsPath);
            if (gradleKtsReq.isPresent()) {
                return gradleKtsReq;
            }
        }

        return Optional.empty();
    }

    private Optional<Requirement> readFromPomXml(Path pomXmlPath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomXmlPath.toFile());

            // Priority 1: maven.compiler.release
            String release = getPropertyValue(doc, "maven.compiler.release");
            if (release != null && !release.isEmpty()) {
                Integer major = VersionParser.parseJavaVersion(release);
                return Optional.of(new Requirement("java", "pom.xml", release, major));
            }

            // Priority 2: java.version
            String javaVersion = getPropertyValue(doc, "java.version");
            if (javaVersion != null && !javaVersion.isEmpty()) {
                Integer major = VersionParser.parseJavaVersion(javaVersion);
                return Optional.of(new Requirement("java", "pom.xml", javaVersion, major));
            }

            // Priority 3: maven.compiler.target (in properties)
            String target = getPropertyValue(doc, "maven.compiler.target");
            if (target != null && !target.isEmpty()) {
                Integer major = VersionParser.parseJavaVersion(target);
                return Optional.of(new Requirement("java", "pom.xml", target, major));
            }

            // Priority 4: maven.compiler.target in plugin configuration
            String pluginTarget = getPluginConfigValue(doc, "maven-compiler-plugin", "target");
            if (pluginTarget != null && !pluginTarget.isEmpty()) {
                Integer major = VersionParser.parseJavaVersion(pluginTarget);
                return Optional.of(new Requirement("java", "pom.xml", pluginTarget, major));
            }

        } catch (Exception e) {
            // Return empty on any parsing error
        }

        return Optional.empty();
    }

    private String getPropertyValue(Document doc, String propertyName) {
        NodeList properties = doc.getElementsByTagName("properties");
        if (properties.getLength() == 0) {
            return null;
        }

        Element propertiesElement = (Element) properties.item(0);
        NodeList propertyNodes = propertiesElement.getChildNodes();
        for (int i = 0; i < propertyNodes.getLength(); i++) {
            Node node = propertyNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (propertyName.equals(element.getTagName())) {
                    return element.getTextContent().trim();
                }
            }
        }
        return null;
    }

    private String getPluginConfigValue(Document doc, String pluginArtifactId, String configName) {
        NodeList plugins = doc.getElementsByTagName("plugin");
        for (int i = 0; i < plugins.getLength(); i++) {
            Element plugin = (Element) plugins.item(i);
            NodeList artifactIds = plugin.getElementsByTagName("artifactId");
            if (artifactIds.getLength() > 0) {
                String artifactId = artifactIds.item(0).getTextContent().trim();
                if (pluginArtifactId.equals(artifactId)) {
                    NodeList configurations = plugin.getElementsByTagName("configuration");
                    if (configurations.getLength() > 0) {
                        Element config = (Element) configurations.item(0);
                        NodeList targets = config.getElementsByTagName(configName);
                        if (targets.getLength() > 0) {
                            return targets.item(0).getTextContent().trim();
                        }
                    }
                }
            }
        }
        return null;
    }

    private Optional<Requirement> readFromBuildGradle(Path buildGradlePath) {
        try {
            String content = Files.readString(buildGradlePath);
            var matcher = GRADLE_SOURCE_COMPATIBILITY_PATTERN.matcher(content);
            if (matcher.find()) {
                String version = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                Integer major = VersionParser.parseJavaVersion(version);
                String fileName = buildGradlePath.getFileName().toString();
                return Optional.of(new Requirement("java", fileName, version, major));
            }
        } catch (IOException e) {
            // Return empty on any error
        }

        return Optional.empty();
    }
}
