package org.jabref.architecture;

import java.nio.file.Paths;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.library.GeneralCodingRules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "org.jabref")
class MainArchitectureTests {

    public static final String CLASS_ORG_JABREF_GLOBALS = "org.jabref.gui.Globals";
    private static final String PACKAGE_JAVAX_SWING = "javax.swing..";
    private static final String PACKAGE_JAVA_AWT = "java.awt..";
    private static final String PACKAGE_ORG_JABREF_GUI = "org.jabref.gui..";
    private static final String PACKAGE_ORG_JABREF_LOGIC = "org.jabref.logic..";
    private static final String PACKAGE_ORG_JABREF_MODEL = "org.jabref.model..";
    private static final String PACKAGE_ORG_JABREF_CLI = "org.jabref.cli..";

    @ArchTest
    public static void doNotUseApacheCommonsLang3(JavaClasses classes) {
        noClasses().that().areNotAnnotatedWith(ApacheCommonsLang3Allowed.class)
                   .should().accessClassesThat().resideInAPackage("org.apache.commons.lang3")
                   .check(classes);
    }

    @ArchTest
    public static void doNotUseSwing(JavaClasses classes) {
        // This checks for all Swing packages, but not the UndoManager
        noClasses().that().areNotAnnotatedWith(AllowedToUseSwing.class)
                   .should().accessClassesThat()
                   .resideInAnyPackage("javax.swing",
                                       "javax.swing.border..",
                                       "javax.swing.colorchooser..",
                                       "javax.swing.event..",
                                       "javax.swing.filechooser..",
                                       "javax.swing.plaf..",
                                       "javax.swing.table..",
                                       "javax.swing.text..",
                                       "javax.swing.tree..")
                   .check(classes);
    }

    @ArchTest
    public static void doNotUseAssertJ(JavaClasses classes) {
        noClasses().should().accessClassesThat().resideInAPackage("org.assertj..")
                   .check(classes);
    }

    @ArchTest
    public static void doNotUseJavaAWT(JavaClasses classes) {
        noClasses().that().areNotAnnotatedWith(AllowedToUseAwt.class)
                   .should().accessClassesThat().resideInAPackage(PACKAGE_JAVA_AWT)
                   .check(classes);
    }

    @ArchTest
    public static void doNotUsePaths(JavaClasses classes) {
        noClasses().should()
                   .accessClassesThat()
                   .belongToAnyOf(Paths.class)
                   .because("Path.of(...) should be used instead")
                   .check(classes);
    }

    @ArchTest
    @ArchIgnore
    // Fails currently
    public static void respectLayeredArchitecture(JavaClasses classes) {
        layeredArchitecture()
                             .layer("Gui").definedBy(PACKAGE_ORG_JABREF_GUI)
                             .layer("Logic").definedBy(PACKAGE_ORG_JABREF_LOGIC)
                             .layer("Model").definedBy(PACKAGE_ORG_JABREF_MODEL)
                             .layer("Cli").definedBy(PACKAGE_ORG_JABREF_CLI)
                             .layer("Migrations").definedBy("org.jabref.migrations..") // TODO: Move to logic
                             .layer("Preferences").definedBy("org.jabref.preferences..")
                             .layer("Styletester").definedBy("org.jabref.styletester..")

                             .whereLayer("Gui").mayOnlyBeAccessedByLayers("Preferences", "Cli") // TODO: Remove preferences here
                             .whereLayer("Logic").mayOnlyBeAccessedByLayers("Gui", "Cli", "Model", "Migrations", "Preferences")
                             .whereLayer("Model").mayOnlyBeAccessedByLayers("Gui", "Logic", "Migrations", "Cli", "Preferences")
                             .whereLayer("Cli").mayNotBeAccessedByAnyLayer()
                             .whereLayer("Migrations").mayOnlyBeAccessedByLayers("Logic")
                             .whereLayer("Preferences").mayOnlyBeAccessedByLayers("Gui", "Logic", "Migrations", "Styletester", "Cli") // TODO: Remove logic here

                             .check(classes);
    }

    @ArchTest
    public static void doNotUseLogicInModel(JavaClasses classes) {
        noClasses().that().resideInAPackage(PACKAGE_ORG_JABREF_MODEL)
                   .and().areNotAnnotatedWith(AllowedToUseLogic.class)
                   .should().dependOnClassesThat().resideInAPackage(PACKAGE_ORG_JABREF_LOGIC)
                   .check(classes);
    }

    @ArchTest
    public static void restrictUsagesInModel(JavaClasses classes) {
        // Until we switch to Lucene, we need to access Globals.stateManager().getActiveDatabase() from the search classes,
        // because the PDFSearch needs to access the index of the corresponding database
        noClasses().that().areNotAssignableFrom("org.jabref.model.search.rules.ContainBasedSearchRule")
                   .and().areNotAssignableFrom("org.jabref.model.search.rules.RegexBasedSearchRule")
                   .and().areNotAssignableFrom("org.jabref.model.search.rules.GrammarBasedSearchRule")
                   .and().resideInAPackage(PACKAGE_ORG_JABREF_MODEL)
                   .should().dependOnClassesThat().resideInAPackage(PACKAGE_JAVAX_SWING)
                   .orShould().dependOnClassesThat().haveFullyQualifiedName(CLASS_ORG_JABREF_GLOBALS)
                   .check(classes);
    }

    @ArchTest
    public static void restrictUsagesInLogic(JavaClasses classes) {
        noClasses().that().resideInAPackage(PACKAGE_ORG_JABREF_LOGIC)
                   .should().dependOnClassesThat().resideInAPackage(PACKAGE_JAVAX_SWING)
                   .orShould().dependOnClassesThat().haveFullyQualifiedName(CLASS_ORG_JABREF_GLOBALS)
                   .check(classes);
    }

    @ArchTest
    public static void restrictStandardStreams(JavaClasses classes) {
        noClasses().that().resideOutsideOfPackages(PACKAGE_ORG_JABREF_CLI)
                   .and().resideOutsideOfPackages("org.jabref.gui.openoffice..") // Uses LibreOffice SDK
                   .and().areNotAnnotatedWith(AllowedToUseStandardStreams.class)
                   .should(GeneralCodingRules.ACCESS_STANDARD_STREAMS)
                   .because("logging framework should be used instead or the class be marked explicitly as @AllowedToUseStandardStreams")
                   .check(classes);
    }
}
