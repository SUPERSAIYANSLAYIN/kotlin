fun main(p: String?) {
    // Generates IFNONNULL and GOTO
    while (p == null) {
        "loop"
    }
}

//0 ICONST_0
//0 ICONST_1
//0 ACONST_NULL
//1 IFNONNULL
//1 IF
//1 GOTO