package mb.pie.lang.test.unary;

import mb.pie.api.ExecException;
import org.junit.jupiter.api.Test;

import static mb.pie.lang.test.util.SimpleChecker.assertTaskOutputEquals;

class ToNonNullableValueTest {
    @Test void test() throws Exception {
        assertTaskOutputEquals(new TaskDefsModule_toNonNullableValueTestGen(), main_toNonNullableValue.class, "test string");
    }
}
