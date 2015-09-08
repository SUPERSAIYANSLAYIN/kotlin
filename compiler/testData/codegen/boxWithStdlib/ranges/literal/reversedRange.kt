// Auto-generated by org.jetbrains.kotlin.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
import java.util.ArrayList

fun box(): String {
    val list1 = ArrayList<Int>()
    for (i in (3..5).reversed()) {
        list1.add(i)
        if (list1.size() > 23) break
    }
    if (list1 != listOf<Int>(5, 4, 3)) {
        return "Wrong elements for (3..5).reversed(): $list1"
    }

    val list2 = ArrayList<Byte>()
    for (i in (3.toByte()..5.toByte()).reversed()) {
        list2.add(i)
        if (list2.size() > 23) break
    }
    if (list2 != listOf<Byte>(5, 4, 3)) {
        return "Wrong elements for (3.toByte()..5.toByte()).reversed(): $list2"
    }

    val list3 = ArrayList<Short>()
    for (i in (3.toShort()..5.toShort()).reversed()) {
        list3.add(i)
        if (list3.size() > 23) break
    }
    if (list3 != listOf<Short>(5, 4, 3)) {
        return "Wrong elements for (3.toShort()..5.toShort()).reversed(): $list3"
    }

    val list4 = ArrayList<Long>()
    for (i in (3.toLong()..5.toLong()).reversed()) {
        list4.add(i)
        if (list4.size() > 23) break
    }
    if (list4 != listOf<Long>(5, 4, 3)) {
        return "Wrong elements for (3.toLong()..5.toLong()).reversed(): $list4"
    }

    val list5 = ArrayList<Char>()
    for (i in ('a'..'c').reversed()) {
        list5.add(i)
        if (list5.size() > 23) break
    }
    if (list5 != listOf<Char>('c', 'b', 'a')) {
        return "Wrong elements for ('a'..'c').reversed(): $list5"
    }

    val list6 = ArrayList<Double>()
    for (i in (3.0..5.0).reversed()) {
        list6.add(i)
        if (list6.size() > 23) break
    }
    if (list6 != listOf<Double>(5.0, 4.0, 3.0)) {
        return "Wrong elements for (3.0..5.0).reversed(): $list6"
    }

    val list7 = ArrayList<Float>()
    for (i in (3.0.toFloat()..5.0.toFloat()).reversed()) {
        list7.add(i)
        if (list7.size() > 23) break
    }
    if (list7 != listOf<Float>(5.0.toFloat(), 4.0.toFloat(), 3.0.toFloat())) {
        return "Wrong elements for (3.0.toFloat()..5.0.toFloat()).reversed(): $list7"
    }

    return "OK"
}
