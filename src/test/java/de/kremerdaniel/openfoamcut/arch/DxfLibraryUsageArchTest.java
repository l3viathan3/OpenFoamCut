package de.kremerdaniel.openfoamcut.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture test to ensure DXF library usage is restricted.
 */
@AnalyzeClasses(packages = "de.kremerdaniel.openfoamcut")
public class DxfLibraryUsageArchTest {

    private static final String BASE = "de.kremerdaniel.openfoamcut.";

    private static final String PARSER_PACKAGE = BASE + "parser..";

    /** Rule ensuring DXF library classes are not used outside parser package. */
    @ArchTest
    public static final ArchRule awt_usage_is_restricted =
        noClasses()
            .that().resideOutsideOfPackage(PARSER_PACKAGE)

            .should().dependOnClassesThat().resideInAnyPackage("org.kabeja..")

            .because("Only the parser package is allowed to use org.kabeja");

}