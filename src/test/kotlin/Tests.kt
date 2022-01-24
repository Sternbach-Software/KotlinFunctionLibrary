
import KotlinFunctionLibrary.formatted
import KotlinFunctionLibrary.println
import KotlinFunctionLibrary.recursiveFind
import org.junit.Test
import org.junit.Assert.*

class Tests {

    @Test
    fun test_recursive_find() {
        class Foo(val a: Char, val b: List<Foo>)
        val list = listOf(
            Foo('a',
                listOf(
                    Foo('b',
                        listOf(
                            Foo('c',
                                listOf()
                            )
                        )
                    ),
                    Foo('d',
                        listOf(
                            Foo('e',
                                listOf(
                                    Foo('f',
                                        listOf()
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        assertTrue(list.recursiveFind({ it.a == 'a' }) { it.b } != null)
        assertTrue(list.recursiveFind({ it.a == 'f' }) { it.b } != null)
        assertFalse(list.recursiveFind({ it.a == 'z'}) { it.b } != null)
    }

    @Test
    fun test_format_time() {

        //0, >1, >1
        Triple(0, 12, 34).formatted(true).let { assert(it == "12:34") }     //0, >10, >10
        Triple(0, 12, 3).formatted(true).let { assert(it == "12:03") }      //0, >10, <10
        Triple(0, 1, 2).formatted(true).let { assert(it == "1:02") }       //0, <10, <10
        Triple(0, 1, 23).formatted(true).let { assert(it == "1:23") }      //0, <10, >10

        //0, 0, >1
        Triple(0, 0, 12).formatted(true).let { assert(it == "00:12") }      //0, 0, >10
        Triple(0, 0, 1).formatted(true).let { assert(it == "00:01") }       //0, 0, <10

        //0, 0, 0
        Triple(0, 0, 0).formatted(true).let { assert(it == "00:00") }       //0, 0, 0

        //>1, 0, 0
        Triple(12, 0, 0).formatted(true).let { assert(it == "12:00:00") }      //>10, 0, 0
        Triple(1, 0, 0).formatted(true).let { assert(it == "1:00:00") }       //<10, 0, 0

        //>1, >1, 0
        Triple(12, 34, 0).formatted(true).let { assert(it == "12:34:00") }     //>10, >10, 0
        Triple(12, 3, 0).formatted(true).let { assert(it == "12:03:00") }      //>10, <10, 0
        Triple(1, 2, 0).formatted(true).let { assert(it == "1:02:00") }       //<10, <10, 0
        Triple(1, 23, 0).formatted(true).let { assert(it == "1:23:00") }      //<10, >10, 0

        //>1, >1, >1
        Triple(12, 34, 56).formatted(true).let { assert(it == "12:34:56") }     //>10, >10, >10
        Triple(12, 34, 5).formatted(true).let { assert(it == "12:34:05") }      //>10, >10, <10
        Triple(12, 3, 4).formatted(true).let { assert(it == "12:03:04") }       //>10, <10, <10
        Triple(12, 3, 45).formatted(true).let { assert(it == "12:03:45") }      //>10, <10, >10
        Triple(1, 23, 45).formatted(true).let { assert(it == "1:23:45") }      //<10, >10, >10
        Triple(1, 23, 4).formatted(true).let { assert(it == "1:23:04") }       //<10, >10, <10
        Triple(1, 2, 3).formatted(true).let { assert(it == "1:02:03") }        //<10, <10, <10
        Triple(1, 2, 34).formatted(true).let { assert(it == "1:02:34") }       //<10, <10, >10
    }
}