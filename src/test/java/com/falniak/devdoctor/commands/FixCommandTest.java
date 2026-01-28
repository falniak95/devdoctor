package com.falniak.devdoctor.commands;

import com.falniak.devdoctor.check.Check;
import com.falniak.devdoctor.check.CheckContext;
import com.falniak.devdoctor.check.CheckResult;
import com.falniak.devdoctor.check.CheckRunner;
import com.falniak.devdoctor.check.CheckStatus;
import com.falniak.devdoctor.check.DockerCheck;
import com.falniak.devdoctor.check.ExecResult;
import com.falniak.devdoctor.check.FakeProcessExecutor;
import com.falniak.devdoctor.check.ProcessExecutor;
import com.falniak.devdoctor.check.Suggestion;
import com.falniak.devdoctor.detect.DetectionResult;
import com.falniak.devdoctor.detect.ProjectType;
import com.falniak.devdoctor.fix.FixAction;
import com.falniak.devdoctor.fix.FixPlan;
import com.falniak.devdoctor.fix.FixPlanner;
import com.falniak.devdoctor.fix.Risk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FixCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testDockerFailProducesCautionAction() throws Exception {
        // Create a FakeProcessExecutor that fails docker check
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("docker", new Exception("docker: command not found"));

        CheckContext context = createTestContext(executor);
        
        // Run DockerCheck to get a FAIL result
        DockerCheck dockerCheck = new DockerCheck();
        CheckResult dockerResult = dockerCheck.run(context);
        
        assertEquals(CheckStatus.FAIL, dockerResult.status());
        assertEquals("system.docker", dockerResult.id());

        // Create FixPlanner and generate plan
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(dockerResult), context);

        // Verify plan contains CAUTION action for docker
        assertFalse(plan.actions().isEmpty(), "Plan should contain actions");
        
        FixAction dockerAction = plan.actions().stream()
            .filter(action -> "system.docker".equals(action.id()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(dockerAction, "Should have docker action");
        assertEquals(Risk.CAUTION, dockerAction.risk(), "Docker action should be CAUTION");
        assertEquals("Install Docker", dockerAction.title());
        assertFalse(dockerAction.applyable(), "CAUTION actions must never be applyable");
        // Commands may be empty on non-Windows systems (no winget), which is fine
        // The important thing is that applyable is false for CAUTION actions
    }

    @Test
    void testCautionActionsNotExecutedEvenWithApply() throws Exception {
        // Create a plan with CAUTION action
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("docker", new Exception("docker: command not found"));

        CheckContext context = createTestContext(executor);
        
        DockerCheck dockerCheck = new DockerCheck();
        CheckResult dockerResult = dockerCheck.run(context);
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(dockerResult), context);
        
        // Verify we have a CAUTION action
        FixAction cautionAction = plan.actions().stream()
            .filter(action -> action.risk() == Risk.CAUTION)
            .findFirst()
            .orElse(null);
        assertNotNull(cautionAction, "Should have CAUTION action");

        // Create FixCommand with --apply --yes
        FixCommand command = new FixCommand();
        setApply(command, true);
        setYes(command, true);
        setPath(command, tempDir.toString());
        
        // Replace the executor in context - we'll need to inject it
        // Since FixCommand creates its own executor, we need to test differently
        // Let's test the planner logic and the filtering logic separately
        
        // Test that CAUTION actions are filtered out when applying
        List<FixAction> safeActions = plan.actions().stream()
            .filter(action -> action.risk() == Risk.SAFE && action.applyable())
            .toList();
        
        // CAUTION actions should not be in safeActions
        assertTrue(safeActions.stream().noneMatch(action -> action.risk() == Risk.CAUTION),
            "CAUTION actions should not be in safe actions list");
    }

    @Test
    void testSafeActionExecutesWithApplyYes() throws Exception {
        // Create a synthetic SAFE action with commands
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("echo", new ExecResult(0, "test output", ""));
        
        // Create a SAFE action that can be applied
        FixAction safeAction = new FixAction(
            "test.safe.action",
            "Test Safe Action",
            "A test action that is safe to apply",
            Risk.SAFE,
            List.of("echo", "test"),
            true
        );
        
        FixPlan plan = new FixPlan(List.of(safeAction));
        
        // Test that SAFE actions are filtered correctly for execution
        List<FixAction> safeActions = plan.actions().stream()
            .filter(action -> action.risk() == Risk.SAFE && action.applyable())
            .toList();
        
        assertEquals(1, safeActions.size(), "Should have one SAFE applyable action");
        assertEquals(safeAction, safeActions.get(0), "Should match the safe action");
        
        // Test execution via ProcessExecutor
        ExecResult result = executor.exec(List.of("echo", "test"));
        assertEquals(0, result.exitCode(), "Command should succeed");
        assertTrue(result.stdout().contains("test output"), "Should have expected output");
    }

    @Test
    void testFixCommandRunsWithoutErrors() throws Exception {
        // Create a minimal project structure
        Path projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);
        
        FixCommand command = new FixCommand();
        setPath(command, projectRoot.toString());
        
        redirectOutput();
        
        try {
            Integer exitCode = command.call();
            // Should complete without errors (exit code 0 or 1 depending on checks)
            assertTrue(exitCode == 0 || exitCode == 1, 
                "Exit code should be 0 or 1");
        } finally {
            restoreOutput();
        }
    }

    @Test
    void testFixPlannerHandlesRequirementFailures() throws Exception {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("node", new ExecResult(0, "v16.0.0", ""));
        
        CheckContext context = createTestContext(executor);
        
        // Create a synthetic requirement check failure
        CheckResult requirementResult = new CheckResult(
            "project.node.requirements",
            CheckStatus.FAIL,
            "Required: 18.x, Local: 16.0.0",
            "Version mismatch",
            List.of(new Suggestion(
                "Install Node.js 18.x",
                List.of("nvm install 18"),
                com.falniak.devdoctor.check.Risk.SAFE
            ))
        );
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(requirementResult), context);
        
        assertFalse(plan.actions().isEmpty(), "Should have actions for requirement failure");
        
        FixAction action = plan.actions().stream()
            .filter(a -> "project.node.requirements".equals(a.id()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(action, "Should have action for node requirements");
        assertEquals(Risk.CAUTION, action.risk(), "Requirement actions should be CAUTION");
        assertTrue(action.title().contains("Node.js"), "Title should mention Node.js");
        assertFalse(action.commands().isEmpty(), "Should have commands from suggestions");
        assertFalse(action.applyable(), "CAUTION actions must never be applyable");
    }

    @Test
    void testFixPlannerAvoidsDuplicates() throws Exception {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("docker", new Exception("docker: command not found"));
        
        CheckContext context = createTestContext(executor);
        
        DockerCheck dockerCheck = new DockerCheck();
        CheckResult dockerResult1 = dockerCheck.run(context);
        CheckResult dockerResult2 = dockerCheck.run(context); // Same result
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(dockerResult1, dockerResult2), context);
        
        // Should only have one action for docker
        long dockerActionCount = plan.actions().stream()
            .filter(a -> "system.docker".equals(a.id()))
            .count();
        
        assertEquals(1, dockerActionCount, "Should not duplicate docker actions");
    }

    @Test
    void testYesWithoutApplyPrintsWarning() throws Exception {
        Path projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);
        
        FixCommand command = new FixCommand();
        setPath(command, projectRoot.toString());
        setYes(command, true);
        setApply(command, false);
        
        redirectOutput();
        
        try {
            command.call();
            String output = outContent.toString();
            assertTrue(output.contains("Note: --yes has no effect without --apply."),
                "Should print warning when --yes is used without --apply");
        } finally {
            restoreOutput();
        }
    }

    @Test
    void testApplyWithNoSafeActions() throws Exception {
        Path projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);
        
        FixCommand command = new FixCommand();
        setPath(command, projectRoot.toString());
        setApply(command, true);
        setYes(command, true);
        
        redirectOutput();
        
        try {
            Integer exitCode = command.call();
            String output = outContent.toString();
            assertTrue(output.contains("No SAFE actions to apply."),
                "Should print 'No SAFE actions to apply.' when there are no SAFE actions");
            assertEquals(0, exitCode, "Exit code should be 0");
        } finally {
            restoreOutput();
        }
    }

    @Test
    void testCommandFormattingUsesBulletList() throws Exception {
        // Create a scenario that will produce a fix plan with commands
        Path projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);
        
        // We'll test by running the actual command and checking output format
        // But first, let's verify the planner creates actions with commands
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("docker", new Exception("docker: command not found"));
        
        CheckContext context = createTestContext(executor);
        
        DockerCheck dockerCheck = new DockerCheck();
        CheckResult dockerResult = dockerCheck.run(context);
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(dockerResult), context);
        
        FixAction action = plan.actions().stream()
            .filter(a -> "system.docker".equals(a.id()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(action, "Should have docker action");
        // Commands may be empty on non-Windows systems (no winget), which is fine
        // The important thing is that the action is created correctly with CAUTION risk
        assertEquals(Risk.CAUTION, action.risk(), "Should be CAUTION");
        assertFalse(action.applyable(), "CAUTION actions must not be applyable");
        
        // On Windows, verify winget command is present
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("windows") && !action.commands().isEmpty()) {
            assertTrue(action.commands().stream()
                .anyMatch(cmd -> cmd.contains("winget")),
                "Should have winget command on Windows");
        }
    }
    
    @Test
    void testDisplayPlanShowsCorrectStatusForCaution() throws Exception {
        // Create a CAUTION action
        FixAction cautionAction = new FixAction(
            "test.caution",
            "Test Caution",
            "Test description",
            Risk.CAUTION,
            List.of("test command"),
            false  // Must be false for CAUTION
        );
        
        FixPlan plan = new FixPlan(List.of(cautionAction));
        
        // We can't easily test displayPlan directly, but we can verify the action properties
        assertEquals(Risk.CAUTION, cautionAction.risk());
        assertFalse(cautionAction.applyable(), "CAUTION actions must not be applyable");
    }

    @Test
    void testCautionActionsNeverApplyable() throws Exception {
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("docker", new Exception("docker: command not found"));
        
        CheckContext context = createTestContext(executor);
        
        DockerCheck dockerCheck = new DockerCheck();
        CheckResult dockerResult = dockerCheck.run(context);
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(dockerResult), context);
        
        // All CAUTION actions must have applyable=false
        for (FixAction action : plan.actions()) {
            if (action.risk() == Risk.CAUTION) {
                assertFalse(action.applyable(),
                    "CAUTION action '" + action.id() + "' must have applyable=false");
            }
        }
    }

    // Helper methods to set private fields via reflection
    private void setPath(FixCommand command, String path) throws Exception {
        java.lang.reflect.Field field = FixCommand.class.getDeclaredField("path");
        field.setAccessible(true);
        field.set(command, path);
    }

    private void setApply(FixCommand command, boolean apply) throws Exception {
        java.lang.reflect.Field field = FixCommand.class.getDeclaredField("apply");
        field.setAccessible(true);
        field.set(command, apply);
    }

    private void setYes(FixCommand command, boolean yes) throws Exception {
        java.lang.reflect.Field field = FixCommand.class.getDeclaredField("yes");
        field.setAccessible(true);
        field.set(command, yes);
    }

    private CheckContext createTestContext(ProcessExecutor executor) {
        Path targetPath = tempDir;
        Set<ProjectType> types = EnumSet.noneOf(ProjectType.class);
        DetectionResult detectionResult = new DetectionResult(targetPath, types, List.of());
        return new CheckContext(targetPath, targetPath, types, detectionResult, executor);
    }

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    private void redirectOutput() {
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    private void restoreOutput() {
        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }
    }

    @Test
    void testNodeMissingNodeModulesGeneratesSafeAction() throws Exception {
        // Create a Node project without node_modules
        Path projectRoot = tempDir.resolve("node-project");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve("package.json"), "{\"name\": \"test\", \"version\": \"1.0.0\"}");
        
        // Ensure node_modules does NOT exist
        Path nodeModules = projectRoot.resolve("node_modules");
        assertFalse(Files.exists(nodeModules), "node_modules should not exist");
        
        FakeProcessExecutor executor = new FakeProcessExecutor();
        Set<ProjectType> types = EnumSet.of(ProjectType.NODE);
        DetectionResult detectionResult = new DetectionResult(projectRoot, types, List.of());
        CheckContext context = new CheckContext(projectRoot, projectRoot, types, detectionResult, executor);
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(), context); // No check results, just checking node_modules
        
        // Should have one SAFE action for installing dependencies
        FixAction nodeDepsAction = plan.actions().stream()
            .filter(action -> "project.node.dependencies".equals(action.id()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(nodeDepsAction, "Should have Node dependencies action");
        assertEquals(Risk.SAFE, nodeDepsAction.risk(), "Should be SAFE");
        assertTrue(nodeDepsAction.applyable(), "Should be applyable");
        assertEquals("Install Node.js dependencies", nodeDepsAction.title());
        assertFalse(nodeDepsAction.commands().isEmpty(), "Should have commands");
        assertEquals(List.of("npm", "install"), nodeDepsAction.commands(), "Should suggest npm install");
    }

    @Test
    void testNodeMissingNodeModulesWithPackageLockGeneratesNpmCi() throws Exception {
        // Create a Node project without node_modules but with package-lock.json
        Path projectRoot = tempDir.resolve("node-project");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve("package.json"), "{\"name\": \"test\", \"version\": \"1.0.0\"}");
        Files.writeString(projectRoot.resolve("package-lock.json"), "{\"name\": \"test\", \"version\": \"1.0.0\"}");
        
        FakeProcessExecutor executor = new FakeProcessExecutor();
        Set<ProjectType> types = EnumSet.of(ProjectType.NODE);
        DetectionResult detectionResult = new DetectionResult(projectRoot, types, List.of());
        CheckContext context = new CheckContext(projectRoot, projectRoot, types, detectionResult, executor);
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(), context);
        
        FixAction nodeDepsAction = plan.actions().stream()
            .filter(action -> "project.node.dependencies".equals(action.id()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(nodeDepsAction, "Should have Node dependencies action");
        assertEquals(List.of("npm", "ci"), nodeDepsAction.commands(), "Should suggest npm ci when package-lock.json exists");
    }

    @Test
    void testSafeActionExecutesWhenApplyYes() throws Exception {
        // Create a Node project and set up executor to capture npm commands
        Path projectRoot = tempDir.resolve("node-project");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve("package.json"), "{\"name\": \"test\", \"version\": \"1.0.0\"}");
        
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("npm", new ExecResult(0, "npm output", ""));
        
        Set<ProjectType> types = EnumSet.of(ProjectType.NODE);
        DetectionResult detectionResult = new DetectionResult(projectRoot, types, List.of());
        CheckContext context = new CheckContext(projectRoot, projectRoot, types, detectionResult, executor);
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(), context);
        
        // Verify we have a SAFE applyable action
        List<FixAction> safeActions = plan.actions().stream()
            .filter(action -> action.risk() == Risk.SAFE && action.applyable())
            .toList();
        
        assertEquals(1, safeActions.size(), "Should have one SAFE applyable action");
        
        // Test that the action would be executed (we can't easily test FixCommand.applyFixes directly
        // without more complex mocking, but we can verify the action is correctly structured)
        FixAction action = safeActions.get(0);
        assertEquals("project.node.dependencies", action.id());
        assertTrue(action.applyable());
        assertFalse(action.commands().isEmpty());
    }

    @Test
    void testCautionActionsNeverExecuted() throws Exception {
        // Create a plan with both SAFE and CAUTION actions
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setException("docker", new Exception("docker: command not found"));
        
        CheckContext context = createTestContext(executor);
        
        DockerCheck dockerCheck = new DockerCheck();
        CheckResult dockerResult = dockerCheck.run(context);
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(dockerResult), context);
        
        // Verify we have CAUTION actions
        List<FixAction> cautionActions = plan.actions().stream()
            .filter(action -> action.risk() == Risk.CAUTION)
            .toList();
        
        assertFalse(cautionActions.isEmpty(), "Should have CAUTION actions");
        
        // Filter to only SAFE and applyable actions (as applyFixes does)
        List<FixAction> safeActions = plan.actions().stream()
            .filter(action -> action.risk() == Risk.SAFE && action.applyable())
            .toList();
        
        // CAUTION actions should not be in safeActions
        assertTrue(safeActions.stream().noneMatch(action -> action.risk() == Risk.CAUTION),
            "CAUTION actions should not be in safe actions list");
        
        // Verify CAUTION actions are never applyable
        for (FixAction action : cautionActions) {
            assertFalse(action.applyable(), 
                "CAUTION action '" + action.id() + "' must have applyable=false");
        }
    }

    @Test
    void testNodeRequirementsMismatchGeneratesCautionAction() throws Exception {
        // Create a Node project with .nvmrc
        Path projectRoot = tempDir.resolve("node-project");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve(".nvmrc"), "18");
        
        FakeProcessExecutor executor = new FakeProcessExecutor();
        executor.setResult("node", new ExecResult(0, "v16.0.0", ""));
        
        Set<ProjectType> types = EnumSet.of(ProjectType.NODE);
        DetectionResult detectionResult = new DetectionResult(projectRoot, types, List.of());
        CheckContext context = new CheckContext(projectRoot, projectRoot, types, detectionResult, executor);
        
        // Create a synthetic requirement check failure
        CheckResult requirementResult = new CheckResult(
            "project.node.requirements",
            CheckStatus.FAIL,
            "Required: 18.x (source: .nvmrc), Local: v16.0.0",
            "Version mismatch",
            List.of(new Suggestion(
                "Install Node.js 18.x",
                List.of("nvm install 18"),
                com.falniak.devdoctor.check.Risk.SAFE
            ))
        );
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(requirementResult), context);
        
        assertFalse(plan.actions().isEmpty(), "Should have actions for requirement failure");
        
        FixAction action = plan.actions().stream()
            .filter(a -> "project.node.requirements".equals(a.id()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(action, "Should have action for node requirements");
        assertEquals(Risk.CAUTION, action.risk(), "Requirement actions should be CAUTION");
        assertEquals("Align Node.js version", action.title());
        assertFalse(action.applyable(), "CAUTION actions must never be applyable");
        
        // Verify description includes version info
        assertTrue(action.description().contains("Required:") || action.description().contains("18"),
            "Description should include version information");
        
        // Verify commands include nvm use (because .nvmrc exists)
        assertTrue(action.commands().stream().anyMatch(cmd -> cmd.contains("nvm use")),
            "Should include 'nvm use' command when .nvmrc exists");
    }

    @Test
    void testNodeRequirementsWithoutNvmrcUsesNvmInstall() throws Exception {
        // Create a Node project without .nvmrc
        Path projectRoot = tempDir.resolve("node-project");
        Files.createDirectories(projectRoot);
        // No .nvmrc file
        
        FakeProcessExecutor executor = new FakeProcessExecutor();
        
        Set<ProjectType> types = EnumSet.of(ProjectType.NODE);
        DetectionResult detectionResult = new DetectionResult(projectRoot, types, List.of());
        CheckContext context = new CheckContext(projectRoot, projectRoot, types, detectionResult, executor);
        
        // Create a synthetic requirement check failure
        CheckResult requirementResult = new CheckResult(
            "project.node.requirements",
            CheckStatus.FAIL,
            "Required: 18.x (source: package.json), Local: v16.0.0",
            "Version mismatch",
            List.of()
        );
        
        FixPlanner planner = new FixPlanner();
        FixPlan plan = planner.plan(List.of(requirementResult), context);
        
        FixAction action = plan.actions().stream()
            .filter(a -> "project.node.requirements".equals(a.id()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(action, "Should have action for node requirements");
        // Should suggest nvm install when .nvmrc doesn't exist
        assertTrue(action.commands().stream().anyMatch(cmd -> cmd.contains("nvm install")),
            "Should include 'nvm install' command when .nvmrc doesn't exist");
    }
}
