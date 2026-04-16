package de.kremerdaniel.openfoamcut.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture test to ensure AWT usage is restricted.
 */
@AnalyzeClasses(packages = "de.kremerdaniel.openfoamcut")
public class AwtUsageArchTest {

    private static final String BASE = "de.kremerdaniel.openfoamcut.";

    private static final String GUI_PACKAGE = BASE + "gui..";

    /** Rule ensuring AWT classes are not used outside GUI package. */
    @ArchTest
    public static final ArchRule awt_usage_is_restricted =
        noClasses()
            .that().resideOutsideOfPackage(GUI_PACKAGE)
            .and().haveNameNotMatching(
                "de\\.kremerdaniel\\.openfoamcut\\.(OpenFoamCut(\\$.*)?|GuiTooltipTest)"
            )
            .and().areNotAssignableTo(
                de.kremerdaniel.openfoamcut.controller.GenerateGCodeController.class
            )
            .should().dependOnClassesThat()
                .resideInAnyPackage("java.awt..", "javax.swing..")
            .because("Only the gui package + OpenFoamCut main class + GenerateGCodeController are allowed to use AWT/Swing");

}