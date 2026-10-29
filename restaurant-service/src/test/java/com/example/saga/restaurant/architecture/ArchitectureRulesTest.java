package com.example.saga.restaurant.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
@AnalyzeClasses(packages = "com.example.saga.restaurant")
class ArchitectureRulesTest {

    private static int inheritanceDepth(JavaClass clazz) {
        int depth = 0;
        JavaClass current = clazz;
        while (current.getRawSuperclass().isPresent()
                && !"java.lang.Object".equals(current.getRawSuperclass().get().getName())) {
            depth++;
            current = current.getRawSuperclass().get();
        }
        return depth;
    }

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule noDeepInheritance =
            classes().should(new ArchCondition<>("not extend more than two levels") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    if (inheritanceDepth(item) > 2) {
                        events.add(SimpleConditionEvent.violated(item, item.getSimpleName()
                                + " extends more than two levels (" + inheritanceDepth(item) + ")"));
                    }
                }
            }).allowEmptyShould(true);

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule smallInterfaces =
            classes().that().areInterfaces().should(new ArchCondition<>("declare at most 7 methods (ISP)") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    if (item.getMethods().size() > 7) {
                        events.add(SimpleConditionEvent.violated(item, item.getSimpleName()
                                + " declares " + item.getMethods().size() + " methods"));
                    }
                }
            }).allowEmptyShould(true);
}
