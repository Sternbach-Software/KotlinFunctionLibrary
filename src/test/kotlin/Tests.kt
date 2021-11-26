
import KotlinFunctionLibrary.formatted
import KotlinFunctionLibrary.println
import org.junit.Test
import org.junit.Assert.*

class Tests {
    @Test
    fun test_format_time() {

        //0, >1, >1
        Triple(0, 12, 34).formatted(true).println()     //0, >10, >10
        Triple(0, 12, 3).formatted(true).println()      //0, >10, <10
        Triple(0, 1, 2).formatted(true).println()       //0, <10, <10
        Triple(0, 1, 23).formatted(true).println()      //0, <10, >10

        //0, 0, >1
        Triple(0, 0, 12).formatted(true).println()      //0, 0, >10
        Triple(0, 0, 1).formatted(true).println()       //0, 0, <10

        //0, 0, 0
        Triple(0, 0, 0).formatted(true).println()       //0, 0, 0

        //>1, 0, 0
        Triple(12, 0, 0).formatted(true).println()      //>10, 0, 0
        Triple(1, 0, 0).formatted(true).println()       //<10, 0, 0

        //>1, >1, 0
        Triple(12, 34, 0).formatted(true).println()     //>10, >10, 0
        Triple(12, 3, 0).formatted(true).println()      //>10, <10, 0
        Triple(1, 2, 0).formatted(true).println()       //<10, <10, 0
        Triple(1, 23, 0).formatted(true).println()      //<10, >10, 0

        //>1, >1, >1
        Triple(12, 34, 56).formatted(true).println()     //>10, >10, >10
        Triple(12, 34, 5).formatted(true).println()      //>10, >10, <10
        Triple(12, 3, 4).formatted(true).println()       //>10, <10, <10
        Triple(12, 3, 45).formatted(true).println()      //>10, <10, >10
        Triple(1, 23, 45).formatted(true).println()      //<10, >10, >10
        Triple(1, 23, 4).formatted(true).println()       //<10, >10, <10
        Triple(1, 2, 3).formatted(true).println()        //<10, <10, <10
        Triple(1, 2, 34).formatted(true).println()       //<10, <10, >10
    }
}