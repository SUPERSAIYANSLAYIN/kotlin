// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: ProtectedField.java

public abstract class ProtectedField {
    protected String field = "OK";
}

//FILE: test.kt
package test

import ProtectedField

class Derived: ProtectedField() {
    fun getField() = myRun { super.field }
}

fun myRun(f: () -> String) = f()

fun box() = Derived().getField()
