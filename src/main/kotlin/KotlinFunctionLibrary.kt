import DiffUtil.DiffResult
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.system.measureNanoTime


/**
 * Library of Kotlin utility functions. Version: 2.1.0
 * */
@Suppress("UNUSED")
object KotlinFunctionLibrary{
    const val WINDOWS_DIRECTORY_REGEX = "[a-zA-Z]:\\\\(((?![<>:\"/\\\\|?*]).)+((?<![ .])\\\\)?)*"
    const val WINDOWS_FILENAME_REGEX =
        "(?!^(PRN|AUX|CLOCK$|NUL|CON|COM\\d|LPT\\d|\\..*)(\\..+)?$)[^\\x00-\\x1f\\\\<>:\"/|?*;]+"
    const val WINDOWS_FILE_EXTENSION_REGEX = "\\w+"

    /**
     * A variable representing a class's parameters - something which Kotlin does not provide
     * */
    val <T : Any> KClass<T>.parameters: List<String?>?
        get() {
            return primaryConstructor?.parameters?.toListBy { it.name }?.toList()
        }

    /**
     * Lambda to generate a random word of size [wordLength]
     * */
    val randomWord = { wordLength:Int ->
        (1..wordLength)
            .map { (('A'..'Z') + ('a'..'z')).random() }
            .joinToString("")
    }

    /**
     * Lambda to generate a list of size [numWords] containing random words of size [wordLength]
     * */
    val listOfRandomWords = {numWords:Int, wordLength:Int->
        val randomListOfWords = mutableListOf<String>()
        for (i in 1..numWords) randomListOfWords.add(randomWord(wordLength))
        randomListOfWords
    }

    /**
     * A version of print which prints [this] and returns [this], facilitating fluent interfaces, functional programming, and assisting in debugging.
     * Also useful for debugging values without breaking a call change or interrupting the flow of code for logging values.
     * */
    fun <T> T.print() = this.apply { kotlin.io.print(this) }

    /**
     * A version of println which prints [this] and returns [this], facilitating fluent interfaces and functional programming.
     * Also useful for debugging values without breaking a call change or interrupting the flow of code for logging values.
     * */
    fun <T> T.println() = this.apply { kotlin.io.println(this) }

    /**
     * A version of print which which prints [message] and returns [this], facilitating fluent interfaces, functional programming, and assisting in debugging.
     * Intended for debugging values without breaking a call change or interrupting the flow of code for logging values.
     * */
    fun <T, R> T.print(message: R) = this.apply { kotlin.io.print(message) }

    /**
     * A version of println which which prints [message] and returns [this], facilitating fluent interfaces, functional programming, and assisting in debugging.
     * Intended for debugging values without breaking a call change or interrupting the flow of code for logging values.
     * */
    fun <T, R> T.println(message: R) = this.apply { kotlin.io.println(message) }

    /**
     * A version of print which passes [this] into [message] to create the message to pass to print, and returns [this], facilitating fluent interfaces, functional programming, and assisting in debugging.
     * Also useful for debugging values without breaking a call change or interrupting the flow of code for logging values.
     * */
    fun <T, R> T.print(message: (T) -> R) = this.apply { kotlin.io.print(message(this)) }

    /**
     * A version of println which passes [this] into [message] to create the message to pass to println, and returns [this], facilitating fluent interfaces, functional programming, and assisting in debugging.
     * Also useful for debugging values without breaking a call change or interrupting the flow of code for logging values.
     * */
    fun <T, R> T.println(message: (T) -> R) = this.apply { kotlin.io.println(message(this)) }

    /**
     *  Returns a list equivalent to appending the contents of [this] to [this] (e.g. listOf(1,2,3).doubled() == listOf(1,2,3,1,2,3) )
     * */
    fun <E> List<E>.doubled(): MutableList<E> = this.toMutableList().also { it.addAll(this) }

    /**
     * Return result of [block] or, if block threw an error (i.e. did not complete), run [onError] and return null. NOTE: Will also return null if [block] completed and returned null
     * */
    fun <T, R> tryAndReturn(
        onError: (e: Throwable, result: R?) -> T = { e, result -> kotlin.io.println("Error thrown; Throwable: $e, Result: $result") as T },
        block: () -> R
    ): R? {
        var result: R? = null
        try {
            result = block()
        } catch (e: Throwable) {
            onError(e, result)
        }
        return result
    }

    /**
     * If [returnErrorBlock] is false, Return result of [block], and if block threw an error (i.e. did not complete), run [onError] and return null.
     * Otherwise, run block, and if it throws an error, run [onError] and return its result.
     * NOTE: Will also return null if [block] completed and returned null
     * @param returnErrorBlock true if caller wants to return the result of the error block in case it is run. Assumes that [onError] will return a nullable version of whatever type [block] returns
     * */
    fun <T, R> tryAndReturn(
        returnErrorBlock: Boolean,
        onError: (e: Throwable, result: R?) -> T = { e, result -> kotlin.io.println("Throwable: $e, Result: $result") as T },
        block: () -> R
    ): R? {
        var result: R? = null
        try {
            result = block()
        } catch (e: Throwable) {
            if (returnErrorBlock) result = onError(e, result) as R? else onError(e, result)
        }
        return result
    }

    /**
     *  A version of [measureNanoTime] which relieves the need to store the execution time of [block] and print it (and do the math to print the result in seconds if desired).
     *  Does not return the result of [block]; see {@link #measureNanoTimeAndPrintAndReturnResult(Boolean, String, () -> T)}
     *  @param message the message to be displayed when printing the time to complete. Will be passed to System.out.printf, so can use "%d"
     *  as the template/placeholder for the time to complete
     * */
    fun measureNanoTimeAndPrintWithoutReturningResult(
        inSeconds: Boolean = false,
        message: String = if (inSeconds) "Time to complete: %d seconds" else "Time to complete: %d nanoseconds",
        block: () -> Unit
    ) {
        val timeToComplete = measureNanoTime(block)
        System.out.printf(message, if(inSeconds) timeToComplete / 1_000_000_000.00 else timeToComplete)
    }

    /**
     *  A version of [measureNanoTime] which relieves the need to store the result and execution time of [block] and print it (and do the math to print the result in seconds if desired).
     *  Does return the result of [block];see {@link #measureNanoTimeAndPrintWithoutReturningResult(Boolean, String, () -> Unit)}
     *  @param message the message to be displayed when printing the time to complete. Will be passed to System.out.printf, so can use "%d"
     *  as the template/placeholder for the time to complete
     *  @see #measureNanoTimeAndPrintAndReturnResult(Boolean, String, T, (T) -> R)}
     * */
    fun <T> measureNanoTimeAndPrintAndReturnResult(
        inSeconds: Boolean = false,
        message: String = if (inSeconds) "Time to complete: %d seconds" else "Time to complete: %d nanoseconds",
        block: () -> T
    ): T {
        var result: T
        val timeToComplete = measureNanoTime { result = block() }
        System.out.printf(message, if(inSeconds) timeToComplete / 1_000_000_000.00 else timeToComplete)
        return result
    }

    /**
     *  A version of [measureNanoTime] which relieves the need to store the result and execution time of [block] and print it (and do the math to print the result in seconds if desired).
     *  Does return the result of [block]; see {@link #measureNanoTimeAndPrintWithoutReturningResult(Boolean, String, () -> Unit)}
     *  @param message the message to be displayed when printing the time to complete. Will be passed to System.out.printf, so can use "%d"
     *  as the template/placeholder for the time to complete
     *  @param param parameter to pass to [block]
     * */
    fun <T, R> measureNanoTimeAndPrintAndReturnResult(
        inSeconds: Boolean = false,
        message: String = if (inSeconds) "Time to complete: %d seconds" else "Time to complete: %d nanoseconds",
        param: T,
        block: (T) -> R
    ): R {
        var result: R
        val timeToComplete = measureNanoTime { result = block(param) }
        System.out.printf(
            message,
            if (inSeconds) timeToComplete / 1_000_000_000.00 else timeToComplete
        )
        return result
    }

    /**
     * Request input from the user by first printing [firstMessageToDisplay] and then calling readLine()
     * Loops for input until the input matches the provided [regex], printing [messageToDisplayOnError] every time the user enters an invalid input until a valid input is entered
     * @param regex the regex to check the input against
     * @param firstMessageToDisplay message to be displayed before input is requested; The string ": " will be appended.
     * @param messageToDisplayOnError message to be displayed when user's input does not  match [regex]; The string ": " will be appended.
     * @return an input from the user which matches [predicate]
     * */
    fun getValidatedInput(
        regex: Regex,
        firstMessageToDisplay: String,
        messageToDisplayOnError: String
    ): String? {
        kotlin.io.print("$firstMessageToDisplay: ")
        var input = readLine()
        while (input?.matches(regex)
                ?.not() == true
        ) /*doesn't match regex (written in a roundabout way to retain nullability)*/ {
            kotlin.io.print("$messageToDisplayOnError: ")
            input = readLine()
        }
        return input
    }

    /**
     * Request input from the user by first printing [firstMessageToDisplay] and then calling readLine()
     * Loops for input until the input matches the provided [predicate] (i.e. the predicate returns true when passed `input`), printing [messageToDisplayOnError] every time the user enters an invalid input until a valid input is entered
     * @param firstMessageToDisplay message to be displayed before input is requested; The string ": " will be appended.
     * @param messageToDisplayOnError message to be displayed when user's input does not  match [predicate]; The string ": " will be appended.
     * @param predicate function that takes the input and returns the result of predicate evaluation on the input.
     * @return an input from the user which matches [predicate]
     * */
    fun getValidatedInput(
        firstMessageToDisplay: String,
        messageToDisplayOnError: String,
        predicate: (String?) -> Boolean
    ): String? {
        kotlin.io.print("$firstMessageToDisplay: ")
        var input = readLine()
        while (!predicate(input)) /*input does not match condition dictated by predicate*/ {
            kotlin.io.print("$messageToDisplayOnError: ")
            input = readLine()
        }
        return input
    }

    /**
     * A variation of String.indexOf() which takes a regex [Pattern] instead of a [CharSequence]
     *  @return index of pattern in [this, or -1 if not found
     */
    fun String?.indexOf(pattern: Pattern): Int {
        val matcher: Matcher = pattern.matcher(toString())
        return if (matcher.find()) matcher.start() else -1
    }
    /**
     * Takes a [Triple] of <hour,minute,second> and returns either e.g. "05:32:15", or "5 hr 32 min 15 sec"
     * */
    fun Triple<Int, Int, Int>.formatted(withColons: Boolean) =
        if (withColons) "${if(this.first.toString().length==1) "0${this.first}" else "${this.first}"}:${this.second}:${this.third}"
        else timeFormattedConcisely(
            this.first,
            this.second,
            this.third
        )

    /**
     * Takes an hour, minute, and second, and will return a string with only those values which are not equal to 0 (e.g. "5 hr 15 sec", "5 hr 32 min 15 sec")
     * */
    fun timeFormattedConcisely(hour: Int, minute: Int, second: Int): String {
        val string = StringBuilder()
        if (hour != 0) string.append("$hour hr ")
        if (minute != 0) string.append("$minute min ")
        if (second != 0) string.append("$second sec")
        return string.toString().trim()
    }

    fun toSeconds(hour: Int, minute: Int, second: Int) =
        (hour * 3600) + (minute * 60) + second

    /**
     * Converts seconds ([this]) to hours, minutes, seconds format
     * @receiver seconds to convert
     * @return a [Triple] of final hour, minute, second
     * @sample (578).toHrMinSec() = (0, 9, 38)*/
    fun Int.toHrMinSec(): Triple<Int, Int, Int> {
        var hour = 0
        var minute = 0
        var second = this
        minute += (second / 60)
        hour += (minute / 60)
        second %= 60
        minute %= 60
        return Triple(hour, minute, second)
    }

    /**
     * Takes hours, minutes, seconds, and converts it to a [Triple] of the form <hour,minute,second>
     * */
    fun toHrMinSec(hour: Int = 0, minute: Int = 0, second: Int = 0): Triple<Int, Int, Int> {
        var minute1 = minute
        var second1 = second
        var hour1 = hour
        minute1 += (second1 / 60)
        hour1 += (minute1 / 60)
        second1 %= 60
        minute1 %= 60
        return Triple(hour1, minute1, second1)
    }

    /**
     * Sorts a list by mutliple criteria
     * NOTE: mutates the provided list in the process
     * @sample  sort(
    myList,
    listOf(Class::myParameter1.name, Class::myParameter2.name),
    listOf(true, false)
    )
     */
    @JvmName("sortWithListGivenAsParameters")
    inline fun <reified T> sort(
        workingList: MutableList<T>,
        sortCriteria: List<String>,
        ascending: List<Boolean>
    ) {
        /*//unoptimized version:
        val oneOfMyClasses = workingList[0]::class
        val firstSelector = oneOfMyClasses.getPropertyToSortBy(shiurFilterOptions[0])
        val compareBy = getComparator(ascending, firstSelector, shiurFilterOptions, oneOfMyClasses)
        workingList.sortWith(compareBy)*/
        //optimized version:

        val size = requireNotEmptyAndSameSize(ascending, sortCriteria,"List of ascending/descending must not be empty",
            "List of sort criteria must not be empty",
            "Each sort criteria must be matched with a ascending/descending boolean; size of ascending/descending list: %d, Size of sort criteria list: %d ")

        workingList.sortWith(
            PRIVATEgetComparator(
                ascending,
                PRIVATEgetPropertyToSortBy(sortCriteria[0]),
                sortCriteria,
                size.second
            )
        )

    }

    /**
     *
     * much faster than passing in sort criteria strings
     *  * @sample  sort(
    myList,
    listOf(Class::myParameter1 as KProperty1<Class, Comparable<Any>>,
    Class::myParameter2 as KProperty1<Class, Comparable<Any>>),
    listOf(true, false)
    )
     * */
    inline fun <reified T> sort(
        workingList: MutableList<T>,
        sortCriteria: List<KProperty1<T, Comparable<Any>?>>,
        ascending: List<Boolean>
    ) {
        val size = requireNotEmptyAndSameSize(ascending, sortCriteria,"List of ascending/descending must not be empty",
            "List of sort criteria must not be empty",
            "Each sort criteria must be matched with a ascending/descending boolean; size of ascending/descending list: %d, Size of sort criteria list: %d ")
        workingList.sortWith(
            PRIVATEgetComparator(
                ascending,
                sortCriteria[0],
                sortCriteria,
                size.second
            )
        )
    }
    /**
     * Wrapper to {@link sort(MutableList<T>,List<KProperty1<T, Comparable<Any>?>>,List<Boolean>)} using [Map] for a more convenient API
     * */
    inline fun <reified T> sort(
        workingList: MutableList<T>,
        sortCriteriaMappedToAscending: Map<KProperty1<T, Comparable<Any>?>,Boolean>
    ) = sort(workingList, sortCriteriaMappedToAscending.keys.toList(), sortCriteriaMappedToAscending.values.toList())

    /**
     * Wrapper to {@link sort(MutableList<T>,List<String>,List<Boolean>)} using [Map] for a more convenient API
     * */
    @JvmName("sortWithClassParameterStrings")
    inline fun <reified T> sort(
        workingList: MutableList<T>,
        sortCriteriaMappedToAscending: Map<String,Boolean>
    ) = sort(workingList, sortCriteriaMappedToAscending.keys.toMutableList(), sortCriteriaMappedToAscending.values.toList())

    /**
     * Wrapper to {@link sort(MutableList<T>,List<KProperty1<T, Comparable<Any>?>>,List<Boolean>)} using [Map] for a more convenient API
     * */
    @JvmName("sortWithListAsReceiverAndMapOfKProperty1")
    inline fun <reified T> MutableList<T>.sort(
        map: Map<KProperty1<T, Comparable<Any>?>, Boolean>
    ) = sort(this, map.keys.toList(), map.values.toList())

    /**
     * Wrapper to {@link sort(MutableList<T>,List<String>,List<Boolean>)} using [Map] for a more convenient API
     * */
    @JvmName("sortWithListAsReceiverAndMapOfString")
    inline fun <reified T> MutableList<T>.sort(
        map: Map<String, Boolean>
    ) = sort(this, map.keys.toMutableList(), map.values.toList())
    /**
     * Takes two lists and throws an [IllegalArgumentException] if either of the lists are empty or if they are different sizes.
     * @param aEmptyMessage message to be printed if [listA] is empty; optionally use "%d" as a template/placeholder for the size of [listA]
     * @param bEmptyMessage message to be printed if [listB] is empty; optionally use "%d" as a template/placeholder for the size of [listB]
     * @param notSameSizeMessage message to br printed if lists are not the same size; optionally use the first "%d" as a template/placeholder for the size of [listB]
     * @return a [Pair] of [listA].size, [listB].size
     * */
    fun <A, B> requireNotEmptyAndSameSize(
        listA: Collection<A>, //ascending
        listB: Collection<B>, //sortCriteria
        aEmptyMessage: String = "List A is empty.",
        bEmptyMessage: String = "List B is empty.",
        notSameSizeMessage: String = "Lists are not same size; List A is size %d, List B is size %d."
    ): Pair<Int,Int> {
        val sizeA = listA.size
        val sizeB = listB.size
        require(sizeA > 0) { System.out.printf(aEmptyMessage,sizeA) }
        require(sizeB > 0) { System.out.printf(bEmptyMessage,sizeB) }
        require(sizeB == sizeA) { System.out.printf(notSameSizeMessage,  sizeA, sizeB) }
        return Pair(sizeA,sizeB)
    }

    /**
     * Creates the [Comparator] which is used to filter a list by multiple criteria
     * Can be thought of as a chain resembling something like
     * val comparator = compareBy(LaundryItem::speaker).thenBy { it.title }.thenByDescending { it.length }.thenByDescending { it.series }.thenBy { it.language }
     * which will then be fed into list.sortedWith(comparator), except the calls to thenBy() and thenByDescending() will also be passed [KProperty1]s
     * @param firstSelector used to start the chain of comparators with ascending or descending order;
     * should be the first of the list of conditions to be sorted by. The iteration through the sort criteria
     * will continue with the sort criteria after [firstSelector]
     * Would make explicitly private but Kotlin throws an error

     * */
    @PublishedApi internal inline fun <reified T> PRIVATEgetComparator(
        ascending: List<Boolean>,
        firstSelector: KProperty1<T, Comparable<Any>?>,
        sortCriteria: List<String>,
        size: Int
    ): Comparator<T> {
        var compareBy =
            if (ascending[0]) compareBy(firstSelector) else compareByDescending(firstSelector)
        for (index in 1 until size) {
//        unoptimized:
//        val isAscending = ascending[index]
//        val propertyToSortBy = getPropertyToSortBy<T>(sortCriteria[index])
//        compareBy = if (isAscending) compareBy.thenBy(propertyToSortBy) else compareBy.thenByDescending(propertyToSortBy)
//          optimized:
            compareBy =
                if (ascending[index]) compareBy.thenBy(PRIVATEgetPropertyToSortBy(sortCriteria[index])) else compareBy.thenByDescending(
                    PRIVATEgetPropertyToSortBy(sortCriteria[index])
                )

        }
        return compareBy
    }

    /**
     * Would make explicitly private but doing so would violate Kotlin access restrictions
     * */
    @JvmName("comparatorWithExplicitKPropertylist")
    @PublishedApi internal fun <T> PRIVATEgetComparator(
        ascending: List<Boolean>,
        firstSelector: KProperty1<T, Comparable<Any>?>,
        sortCriteria: List<KProperty1<T, Comparable<Any>?>>,
        size: Int
    ): Comparator<T> {
        var compareBy =
            if (ascending[0]) compareBy(firstSelector) else compareByDescending(firstSelector)
        for (index in 1 until size) {
            compareBy =
                if (ascending[index]) compareBy.thenBy(sortCriteria[index]) else compareBy.thenByDescending(
                    sortCriteria[index]
                )

        }
        return compareBy
    }

    /**
     * Would make explicitly private but doing so would violate Kotlin access restrictions
     * @return null if no parameter was found
     * */
    @PublishedApi internal inline fun <reified T> PRIVATEgetPropertyToSortBy(
        sortCriterion: String
    ): KProperty1<T, Comparable<Any>?> =
        (T::class as KClass<*>).memberProperties.find { it.name == sortCriterion } as KProperty1<T, Comparable<Any>?>?
            ?: throw IllegalArgumentException("Parameter \"$sortCriterion\" not found")

    /**
     * Returns a list containing the results of applying the given [transform] function
     * to each element in the original collection.
     * */
    fun <T, R> Iterable<T>.toListBy(transform: (T) -> R): MutableList<R> {
        val mutableList: MutableList<R> = mutableListOf()
        forEach { mutableList.add(transform(it)) }
        return mutableList
    }

    /**
     * @param nullable to distinguish between nullable and not
     * */
    fun <T, R> Iterable<T?>.toListBy(nullable: Boolean, transform: (T) -> R): MutableList<R> {
        val mutableList: MutableList<R> = mutableListOf()
        forEach { it?.let { it1 -> mutableList.add(transform(it1)) } }
        return mutableList
    }

    /**
     * Mutates the reciever list to exactly match [other]. Does not take into account whether contents are identical but have moved,
     * nor whether a region of the lists are matching, but one is missing the contents of another, and just overrides all of the rest
     * of it instead of inserting the missing elements. (e.g. [1,2,4,5].convertTo([1,2,3,4,5]) will do 3 operations instead of 1 (two changes and one addition))
    TODO make version of this function which uses [DiffUtil] to make it more efficient
     * */
/*Test:
*
* /*    val originalList = listOf("aa","bba","cc","bbc","bbca")
val workingList = mutableListOf("aa", "bba", "bbca")
val workingList1 = mutableListOf("aa","bba","cc","bbc","bbcab")
val originalList1 = mutableListOf("aa", "bba", "bbca")
val workingList2 = mutableListOf("aa","cc","bba")
val originalList2 = mutableListOf("aa", "bba", "bbca")
println("workingList=$workingList")
println("originalList=$originalList")
workingList.simpleConvertToMy(originalList)
println("workingList=$workingList")
println()
println("workingList1=$workingList1")
println("originalList1=$originalList1")
workingList1.simpleConvertToMy(originalList1)
println("workingList1=$workingList1")
println()
println("workingList2=$workingList2")
println("originalList2=$originalList2")
workingList2.simpleConvertToMy(originalList2)
println("workingList2=$workingList2")*/
*
* */
    fun MutableList<String>.convertTo(
        other: List<String>
    ) {
        //TODO would using clear() and addAll() be more efficient?
        val size1 = this.size
        val size2 = other.size
        when {
            size2 > size1 -> {
                this.toList().forEachIndexed { index: Int, s: String ->
                    this[index] =
                        other[index] //TODO would this be more efficient by checking whether they are already equal?
                }
                for (index in size1 until size2) this.add(other[index])
            }
            size2 == size1 -> {
                this.toList().forEachIndexed { index: Int, s: String ->
                    this[index] =
                        other[index] //TODO would this be more efficient by checking whether they are already equal?
                }
            }
            size2 < size1 -> {
                for (counter in size2 until size1) this.removeAt(size2) //constantly remove the "size2"th element until they are the same size
                other.forEachIndexed { index, s ->
                    this[index] = s
                }
            }
        }
    }

    /*Test:
    *  val mutableListOf = mutableListOf(true, true, true)
    println(mutableListOf)
    mutableListOf.myReplaceAll{false}
    println(mutableListOf)*/

    /**
     * Version of {@link MutableList#replaceAll(UnaryOperator)} which compiles to earlier versions of Kotlin and Android by avoiding the use of UnaryOperator
     * */
    fun <E> MutableList<E>.replaceAll(operator: (E?) -> E) {
        val li: MutableListIterator<E> = this.listIterator()
        while (li.hasNext()) {
            li.set(operator(li.next()))
        }
    }

    /**
     * <p>Finds the n-th index within a String, handling {@code null}.
     * This method uses {@link String#indexOf(String)} if possible.</p>
     * <p>Note that matches may overlap<p>
     *
     * <p>A {@code null} CharSequence will return {@code -1}.</p>
     *
     * @param searchStr  the CharSequence to find, may be null
     * @param ordinal  the n-th {@code searchStr} to find, overlapping matches are allowed.
     * @param startingFromTheEnd true if lastOrdinalIndexOf() otherwise false if ordinalIndexOf()
     * @return the n-th index of the search CharSequence,
     *  {@code -1} if no match or {@code null} string input for [searchStr]
     */
    fun String.ordinalIndexOf(searchStr: String?, ordinal: Int, startingFromTheEnd: Boolean): Int {
        if (searchStr == null || ordinal <= 0) {
            return -1
        }
        if (searchStr.isEmpty()) {
            return if (startingFromTheEnd) this.length else 0
        }
        var found = 0
        // set the initial index beyond the end of the string
        // this is to allow for the initial index decrement/increment
        var index = if (startingFromTheEnd) this.length else -1
        do {
            index = if (startingFromTheEnd) {
                this.lastIndexOf(searchStr, index - 1) // step backwards thru string
            } else {
                this.indexOf(searchStr, index + 1) // step forwards through string
            }
            if (index < 0) {
                return index
            }
            found++
        } while (found < ordinal)
        return index
    }

    /**
     * Checks whether a string is an instance of an enum
     * */
    fun <E : Enum<E>?> isInEnum(value: String, enumClass: Class<E>): Boolean {
        for (e in enumClass.enumConstants) {
            if (e!!.name.equals(value, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /**
     * Creates a file (or overwrites if already existing and [overwrite] is true), and takes care of releasing the[BufferedWriter]
     * which is passed in to [action].
     * */
    fun createFileAndPerformWrite(filename: String, overwrite: Boolean, action: (BufferedWriter) -> Unit) {
        File(filename).apply {
            createFile(overwrite)
            performWrite(action)
        }
    }

    /**
     * Creates a file (or overwrites if already existing and [overwrite] is true), and takes care of releasing the[BufferedReader]
     * which is passed in to [action].
     * */
    fun createFileAndPerformRead(filename: String, overwrite: Boolean, action: (BufferedReader) -> Unit) {
        File(filename).apply {
            createFile(overwrite)
            performRead(action)
        }
    }

    /**
     * Creates a file (or overwrites if already existing and [overwrite] is true), and takes care of releasing the[BufferedWriter]
     * which is passed in to [action].
     * */
    fun File.createFileAndPerformWrite(overwrite: Boolean, action: (BufferedWriter) -> Unit) {
        createFile(overwrite)
        performWrite(action)
    }

    /**
     * Creates a file (or overwrites if already existing and [overwrite] is true), and takes care of releasing the[BufferedReader]
     * which is passed in to [action].
     * */
    fun File.createFileAndPerformRead(overwrite: Boolean, action: (BufferedReader) -> Unit) {
        createFile(overwrite)
        performRead(action)
    }

    /**
     * Creates a file if non-existant, or overwrites it if [overwrite] is true
     * */
    fun File.createFile(overwrite: Boolean = false): File {
        if (overwrite && exists()) {
            if(isDirectory) deleteRecursively().println("${this.name} deleted recursively.")
            else delete().println("${this.name} deleted.")
        }
        createNewFile()
        return this //to allow for functional programming/ chaining calls
    }
    /**
     * Creates a folder if non-existant, or overwrites it if [overwrite] is true
     * */
    fun File.createFolder(createParentFolders:Boolean = false, overwrite: Boolean = false): File {
        if (overwrite && exists()) deleteRecursively()
        if(createParentFolders) mkdirs() else mkdir()
        return this //to allow for functional programming/ chaining calls
    }

    /**
     * Performs an on a file by passing the file's [bufferedWriter] to [action] and closing it after the action is done
     * */
    fun File.performWrite(action: (BufferedWriter) -> Unit) {
        val writer = bufferedWriter()
        action(writer)
        writer.close()
    }

    // append lines of text
    @Throws(IOException::class)
    fun Path.appendLinesToFile(list: List<String>) {
        Files.write(
            this,
            list,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )
    }

    // write lines of text
    @Throws(IOException::class)
    fun Path.writeLinesToFile(list: List<String>) {
        Files.write(
            this,
            list,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE
        )
    }

    /**
     * Performs an [action] on a file by passing the file's [bufferedReader] to [action] and closing it after the action is done
     * @return the result of the lambda [action]
     * */
    fun <R> File.performRead(action: (BufferedReader) -> R): R {
        val r:R
        val reader = bufferedReader()
        r = action(reader)
        reader.close()
        return r
    }

    /**
     * Appends [append] to [this] if [predicate] returns true when passed [this]
     * Use case:                 it.appendLine("High-risk demographic: ".appendIf("Male(${first.ageRange.first}-${first.ageRange.last})") { demographics.size == 1 }.appendIf("Female(${first.ageRange.first}-${first.ageRange.last}") { demographics.size == 2 } )

     * */
    fun String.appendIf(append: String, predicate: (String) -> Boolean): String {
        return if(predicate(this)) this + append else this
    }

    /**
     * Appends [append] to [this] if [predicate] returns true when passed [this], otherwise appends [else]
     * Use case:                 it.appendLine("High-risk demographic: ".appendIf("Male(${first.ageRange.first}-${first.ageRange.last})","N\\A") { demographics.size == 1 }.appendIf("Female(${first.ageRange.first}-${first.ageRange.last}") { demographics.size == 2 } )

     * */
    fun String.appendIf(append: String, `else`: String, predicate: (String) -> Boolean): String {
        return if(predicate(this)) this + append else this + `else`
    }

    fun <T> myBuildList(capacity:Int, action: () -> T): List<T> {
        val list = mutableListOf<T>()
        for(i in 0 until capacity) list.add(action())
        return list.toList()
    }
    fun <T> myBuildMutableList(capacity:Int, action: () -> T): MutableList<T> {
        val list = mutableListOf<T>()
        for(i in 0 until capacity) list.add(action())
        return list
    }
    /**
     * Returns whether the given CharSequence contains only digits.
     */
    fun CharSequence.isDigitsOnly(): Boolean {
        var cp: Int
        var i = 0
        while (i < length) {
            cp = Character.codePointAt(this, i)
            if (!Character.isDigit(cp)) {
                return false
            }
            i += Character.charCount(cp)
        }
        return true
    }

    fun String.substringBetween(str1: String, str2: String): String {
        val index1 = indexOf(str1)
        return substring(index1 + str1.length, indexOf(str2,index1))
    }

    /**
     *
     * @param returnIndicesFromStartOfString if true, indices will be indexOf(str), otherwise indexOf(str) + str.length
     * @return Triple<string in-between, first index, second index>
     *     The return is this strange data structure because it was created out of the need for the following use case:
     *     I was reading a string which had a number of substrings i had to parse into objects, and I wanted to save
     *     the index of the end of the first substring so that i could use it for the beggining of the next substring,
     *     eliminating the need to find the index again. The flow of calls to substring() were intended to start with
     *     this function and then continue with {@link #substring(Int, String)}
     * */
    fun String.substringBetween(str1: String, str2: String, returnIndicesFromStartOfString: Boolean):Triple<String, Int, Int> {
        require(this.isNotBlank()){"The string passed to subStringBetween as `this` was empty. params: \"$this\".substringBetween(\"$str1\", \"$str2\", \"$returnIndicesFromStartOfString\")"}
        val index1 = indexOf(str1)
        if(index1 < 0) throw StringIndexOutOfBoundsException("String 1 (\"$str1\") was not found in \"$this\".")
        val index2 =  indexOf(str2,index1)
        if(index2 < 0) throw StringIndexOutOfBoundsException("String 2 (\"$str2\") was not found in \"$this\".")
        val indexOfEndOfFirstWord = index1 + str1.length
//        println("this=$this,str1=$str1,str2=$str2")
//        println("index1=$index1,index2=$index2")
        val str = substring(indexOfEndOfFirstWord,index2)
        return Triple(str, if(returnIndicesFromStartOfString) index1  else indexOfEndOfFirstWord, if(returnIndicesFromStartOfString) index2 else index2 + str2.length)
    }

    /**
     * Returns the substring between [startIndex] and the next occurence of [endString] after [startIndex]
     * @return [Pair]<abovementioned substring, index of [endString]>
     * */
    fun String.substring(startIndex:Int, endString: String): Pair<String,Int>{
        val endIndex = indexOf(endString, startIndex)
        return Pair(substring(startIndex,endIndex), endIndex)
    }
    /**
     * Returns the substring between [startIndex] and the next occurence of [endString] after [startIndex]
     * @return [Pair]<abovementioned substring, index of [endString]>
     * */
    fun String.substring(startIndex:Int, endString: String, returnIndexFromStartOfFoundString: Boolean): Pair<String,Int>{
        val endIndex = indexOf(endString, startIndex)
        return Pair(substring(startIndex,endIndex), if(returnIndexFromStartOfFoundString) endIndex else endIndex + endString.length)
    }

    fun toLookBehindMatchAhead(behind:String, match:String, ahead:String): Regex{
        return "(?<=$behind)$match(?=$ahead)".toRegex()
    }
    /**
     * Takes a string of the form \"look_behind~~match~~look_ahead\" and returns a [Regex] with that form (after removing the ~~)
     * */
    fun String.toLookBehindMatchAhead(escapeIllegalCharacters: Boolean = true): Regex{
        require(count{it=='~'}==4)
        var (behind,match,ahead) = split("~~")
        fun String.escapeIllegalCharacters(): String{
            var temp = this
            fun replaceIfContains(vararg strings: String){
                for(string in strings) if(temp.contains(string)) temp = temp.replace(string,"\\$string")
            }
            replaceIfContains("(",")","[","]","{","}")
            return temp
        }
        if(escapeIllegalCharacters){
            behind = behind.escapeIllegalCharacters()
            match = match.escapeIllegalCharacters()
            ahead = ahead.escapeIllegalCharacters()
        }
        return "(?<=$behind)$match(?=$ahead)".toRegex()
    }

    //String diffing

//    fun printDiff(correctString: String, incorrectString: String) {
    /**
     *
     * @return list of missing indices/list of indices of characters which are present in the bigger string of the inputs
     * and missing in the smaller string
     * */
    fun printDiff(str1: String, str2: String): List<Int> {
        val biggerString = if(str1.length>str2.length) str1 else str2
        val smallerString = if(biggerString === str1) str2 else str1
        val missingIndices = getDiff(biggerString,smallerString)
        return printDiff(missingIndices, biggerString)
    }

    private fun printDiff(missingIndices: List<Int>, referenceString: String): List<Int> {
        if (missingIndices.isEmpty()) {
            println("Strings are the same.")
            return emptyList()
        } else {
            val diffSpaced = referenceString
                .mapIndexed { index: Int, c: Char -> if (index in missingIndices) ' ' else c }
                .joinToString("")
            val diffHighlighted = referenceString
                .mapIndexed { index: Int, c: Char -> if (index in missingIndices) '^' else ' ' }
                .joinToString("")
            KotlinFunctionLibrary.println(referenceString)
            KotlinFunctionLibrary.println(diffSpaced)
            KotlinFunctionLibrary.println(diffHighlighted)
            println("Missing indices: $missingIndices")
            return missingIndices
        }
    }

    /**
     *
     * @return list of indices of characters which are present in [str2] and missing in [str1]
     * see printDiff
     * */
    fun getDiff(str1: String, str2: String): List<Int> {
        val charArray1: List<Char> = str1.toList()
        val missingIndices: MutableList<Int> = mutableListOf()

        var currentPosition = 0
        for (char in str2.toList()) {
            val position: Int = charArray1.indexOf(char, currentPosition) ?: continue
            while (currentPosition < position) {
                missingIndices.add(currentPosition)
                currentPosition += 1
            }
            currentPosition = position + 1
        }
        return missingIndices.toList()
    }

    fun <T> List<T>.indexOf(element: T, startIndex: Int): Int? {
        for (i in startIndex until this.size) {
            if(this[i] == element) return i
        }
        return null
    }

    fun <T> Iterable<T>.toFrequencyMap(): Map<T,Int>{
        val frequencies: MutableMap<T, Int> = mutableMapOf()
        this.forEach{ frequencies[it] = frequencies.getOrDefault(it, 0) + 1 }
        return frequencies
    }

    /**
     * Returns a list which contains a copy of [this] [n] times: e.g. listOf(1,2,3).multiplyBy(3){it+1} == listOf(1,2,3, 2,3,4, 2,3,4)
     * */
    fun <E> MutableList<E>.multiplyListBy(n: Int, transform: (E) -> E): MutableList<E> {
        return also {
            val original = it.toList()
            (1 until n).forEach { i -> it.addAll(original.map { it1 -> transform(it1) }) }
        }
    }

    /**
     * A small method to do primitive warming up of the JVM.
     * */
    fun warmUpJVM() {
        val sb = StringBuilder()
        var d = 0.0
        while (d < 1000000) {
            sb.append(d * (.00001 / 8)) //to "warm up" the JVM
            d++
        }
    }

    /**
     * Turns a number into an ordinal string representation (e.g. 4.toOrdinalNumber()== "4th"
     * */
    fun Int.toOrdinalNumber(): String {
        val suffixes = arrayOf("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")
        return when (this % 100) {
            11, 12, 13 -> "${this}th"
            else -> this.toString() + suffixes[this % 10]
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("KotlinFunctionLibrary v2.1.0")
    }
}

/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * DiffUtil is a utility class that calculates the difference between two lists and outputs a
 * list of update operations that converts the first list into the second one.
 *
 *
 * It can be used to calculate updates for a RecyclerView Adapter. See [ListAdapter] and
 * [AsyncListDiffer] which can simplify the use of DiffUtil on a background thread.
 *
 *
 * DiffUtil uses Eugene W. Myers's difference algorithm to calculate the minimal number of updates
 * to convert one list into another. Myers's algorithm does not handle items that are moved so
 * DiffUtil runs a second pass on the result to detect items that were moved.
 *
 *
 * Note that DiffUtil, ListAdapter, and AsyncListDiffer require the list to not mutate while in use.
 * This generally means that both the lists themselves and their elements (or at least, the
 * properties of elements used in diffing) should not be modified directly. Instead, new lists
 * should be provided any time content changes. It's common for lists passed to DiffUtil to share
 * elements that have not mutated, so it is not strictly required to reload all data to use
 * DiffUtil.
 *
 *
 * If the lists are large, this operation may take significant time so you are advised to run this
 * on a background thread, get the [DiffResult] then apply it on the RecyclerView on the main
 * thread.
 *
 *
 * This algorithm is optimized for space and uses O(N) space to find the minimal
 * number of addition and removal operations between the two lists. It has O(N + D^2) expected time
 * performance where D is the length of the edit script.
 *
 *
 * If move detection is enabled, it takes an additional O(N^2) time where N is the total number of
 * added and removed items. If your lists are already sorted by the same constraint (e.g. a created
 * timestamp for a list of posts), you can disable move detection to improve performance.
 *
 *
 * The actual runtime of the algorithm significantly depends on the number of changes in the list
 * and the cost of your comparison methods. Below are some average run times for reference:
 * (The test list is composed of random UUID Strings and the tests are run on Nexus 5X with M)
 *
 *  * 100 items and 10 modifications: avg: 0.39 ms, median: 0.35 ms
 *  * 100 items and 100 modifications: 3.82 ms, median: 3.75 ms
 *  * 100 items and 100 modifications without moves: 2.09 ms, median: 2.06 ms
 *  * 1000 items and 50 modifications: avg: 4.67 ms, median: 4.59 ms
 *  * 1000 items and 50 modifications without moves: avg: 3.59 ms, median: 3.50 ms
 *  * 1000 items and 200 modifications: 27.07 ms, median: 26.92 ms
 *  * 1000 items and 200 modifications without moves: 13.54 ms, median: 13.36 ms
 *
 *
 *
 * Due to implementation constraints, the max size of the list can be 2^26.
 *
 * @see ListAdapter
 *
 * @see AsyncListDiffer
 */
object DiffUtil {
    private val SNAKE_COMPARATOR: Comparator<Snake> = Comparator { o1, o2 ->
        val cmpX = o1.x - o2.x
        if (cmpX == 0) o1.y - o2.y else cmpX
    }
    // Myers' algorithm uses two lists as axis labels. In DiffUtil's implementation, `x` axis is
    // used for old list and `y` axis is used for new list.
    /**
     * Calculates the list of update operations that can covert one list into the other one.
     *
     * @param cb The callback that acts as a gateway to the backing list data
     *
     * @return A DiffResult that contains the information about the edit sequence to convert the
     * old list into the new list.
     */
    fun calculateDiff(cb: Callback): DiffResult {
        return calculateDiff(cb, true)
    }

    /**
     * Calculates the list of update operations that can covert one list into the other one.
     *
     *
     * If your old and new lists are sorted by the same constraint and items never move (swap
     * positions), you can disable move detection which takes `O(N^2)` time where
     * N is the number of added, moved, removed items.
     *
     * @param cb The callback that acts as a gateway to the backing list data
     * @param detectMoves True if DiffUtil should try to detect moved items, false otherwise.
     *
     * @return A DiffResult that contains the information about the edit sequence to convert the
     * old list into the new list.
     */
    fun calculateDiff(cb: Callback, detectMoves: Boolean): DiffResult {
        val oldSize = cb.oldListSize
        val newSize = cb.newListSize
        val snakes: MutableList<Snake> = ArrayList()

        // instead of a recursive implementation, we keep our own stack to avoid potential stack
        // overflow exceptions
        val stack: MutableList<Range> = ArrayList()
        stack.add(Range(0, oldSize, 0, newSize))
        val max = oldSize + newSize + Math.abs(oldSize - newSize)
        // allocate forward and backward k-lines. K lines are diagonal lines in the matrix. (see the
        // paper for details)
        // These arrays lines keep the max reachable position for each k-line.
        val forward = IntArray(max * 2)
        val backward = IntArray(max * 2)

        // We pool the ranges to avoid allocations for each recursive call.
        val rangePool: MutableList<Range> = ArrayList()
        while (!stack.isEmpty()) {
            val range = stack.removeAt(stack.size - 1)
            val snake = diffPartial(
                cb, range.oldListStart, range.oldListEnd,
                range.newListStart, range.newListEnd, forward, backward, max
            )
            if (snake != null) {
                if (snake.size > 0) {
                    snakes.add(snake)
                }
                // offset the snake to convert its coordinates from the Range's area to global
                snake.x += range.oldListStart
                snake.y += range.newListStart

                // add new ranges for left and right
                val left = if (rangePool.isEmpty()) Range() else rangePool.removeAt(
                    rangePool.size - 1
                )
                left.oldListStart = range.oldListStart
                left.newListStart = range.newListStart
                if (snake.reverse) {
                    left.oldListEnd = snake.x
                    left.newListEnd = snake.y
                } else {
                    if (snake.removal) {
                        left.oldListEnd = snake.x - 1
                        left.newListEnd = snake.y
                    } else {
                        left.oldListEnd = snake.x
                        left.newListEnd = snake.y - 1
                    }
                }
                stack.add(left)

                // re-use range for right
                val right = range
                if (snake.reverse) {
                    if (snake.removal) {
                        right.oldListStart = snake.x + snake.size + 1
                        right.newListStart = snake.y + snake.size
                    } else {
                        right.oldListStart = snake.x + snake.size
                        right.newListStart = snake.y + snake.size + 1
                    }
                } else {
                    right.oldListStart = snake.x + snake.size
                    right.newListStart = snake.y + snake.size
                }
                stack.add(right)
            } else {
                rangePool.add(range)
            }
        }
        // sort snakes
        Collections.sort(snakes, SNAKE_COMPARATOR)
        return DiffResult(cb, snakes, forward, backward, detectMoves)
    }

    private fun diffPartial(
        cb: Callback, startOld: Int, endOld: Int,
        startNew: Int, endNew: Int, forward: IntArray, backward: IntArray, kOffset: Int
    ): Snake? {
        val oldSize = endOld - startOld
        val newSize = endNew - startNew
        if (endOld - startOld < 1 || endNew - startNew < 1) {
            return null
        }
        val delta = oldSize - newSize
        val dLimit = (oldSize + newSize + 1) / 2
        Arrays.fill(forward, kOffset - dLimit - 1, kOffset + dLimit + 1, 0)
        Arrays.fill(backward, kOffset - dLimit - 1 + delta, kOffset + dLimit + 1 + delta, oldSize)
        val checkInFwd = delta % 2 != 0
        for (d in 0..dLimit) {
            run {
                var k: Int = -d
                while (k <= d) {

                    // find forward path
                    // we can reach k from k - 1 or k + 1. Check which one is further in the graph
                    var x: Int
                    val removal: Boolean
                    if (k == -d || (k != d && forward.get(kOffset + k - 1) < forward.get(kOffset + k + 1))) {
                        x = forward.get(kOffset + k + 1)
                        removal = false
                    } else {
                        x = forward.get(kOffset + k - 1) + 1
                        removal = true
                    }
                    // set y based on x
                    var y: Int = x - k
                    // move diagonal as long as items match
                    while ((x < oldSize) && (y < newSize
                                ) && cb.areItemsTheSame(startOld + x, startNew + y)
                    ) {
                        x++
                        y++
                    }
                    forward[(kOffset + k)] = x
                    if (checkInFwd && (k >= delta - d + 1) && (k <= delta + d - 1)) {
                        if (forward.get(kOffset + k) >= backward.get(kOffset + k)) {
                            val outSnake: Snake = Snake()
                            outSnake.x = backward.get(kOffset + k)
                            outSnake.y = outSnake.x - k
                            outSnake.size = forward.get(kOffset + k) - backward.get(kOffset + k)
                            outSnake.removal = removal
                            outSnake.reverse = false
                            return outSnake
                        }
                    }
                    k += 2
                }
            }
            var k = -d
            while (k <= d) {

                // find reverse path at k + delta, in reverse
                val backwardK = k + delta
                var x: Int
                val removal: Boolean
                if (backwardK == d + delta || (backwardK != -d + delta
                            && backward[kOffset + backwardK - 1] < backward[kOffset + backwardK + 1])
                ) {
                    x = backward[kOffset + backwardK - 1]
                    removal = false
                } else {
                    x = backward[kOffset + backwardK + 1] - 1
                    removal = true
                }

                // set y based on x
                var y = x - backwardK
                // move diagonal as long as items match
                while (x > 0 && y > 0 && cb.areItemsTheSame(startOld + x - 1, startNew + y - 1)) {
                    x--
                    y--
                }
                backward[kOffset + backwardK] = x
                if (!checkInFwd && k + delta >= -d && k + delta <= d) {
                    if (forward[kOffset + backwardK] >= backward[kOffset + backwardK]) {
                        val outSnake = Snake()
                        outSnake.x = backward[kOffset + backwardK]
                        outSnake.y = outSnake.x - backwardK
                        outSnake.size = forward[kOffset + backwardK] - backward[kOffset + backwardK]
                        outSnake.removal = removal
                        outSnake.reverse = true
                        return outSnake
                    }
                }
                k += 2
            }
        }
        throw IllegalStateException(
            "DiffUtil hit an unexpected case while trying to calculate"
                    + " the optimal path. Please make sure your data is not changing during the"
                    + " diff calculation."
        )
    }

    /**
     * A Callback class used by DiffUtil while calculating the diff between two lists.
     */
    abstract class Callback() {
        /**
         * Returns the size of the old list.
         *
         * @return The size of the old list.
         */
        abstract val oldListSize: Int

        /**
         * Returns the size of the new list.
         *
         * @return The size of the new list.
         */
        abstract val newListSize: Int

        /**
         * Called by the DiffUtil to decide whether two object represent the same Item.
         *
         *
         * For example, if your items have unique ids, this method should check their id equality.
         *
         * @param oldItemPosition The position of the item in the old list
         * @param newItemPosition The position of the item in the new list
         * @return True if the two items represent the same object or false if they are different.
         */
        abstract fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean

        /**
         * Called by the DiffUtil when it wants to check whether two items have the same data.
         * DiffUtil uses this information to detect if the contents of an item has changed.
         *
         *
         * DiffUtil uses this method to check equality instead of [Object.equals]
         * so that you can change its behavior depending on your UI.
         * For example, if you are using DiffUtil with a
         * [RecyclerView.Adapter], you should
         * return whether the items' visual representations are the same.
         *
         *
         * This method is called only if [.areItemsTheSame] returns
         * `true` for these items.
         *
         * @param oldItemPosition The position of the item in the old list
         * @param newItemPosition The position of the item in the new list which replaces the
         * oldItem
         * @return True if the contents of the items are the same or false if they are different.
         */
        abstract fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean

        /**
         * When [.areItemsTheSame] returns `true` for two items and
         * [.areContentsTheSame] returns false for them, DiffUtil
         * calls this method to get a payload about the change.
         *
         *
         * For example, if you are using DiffUtil with [RecyclerView], you can return the
         * particular field that changed in the item and your
         * [ItemAnimator][RecyclerView.ItemAnimator] can use that
         * information to run the correct animation.
         *
         *
         * Default implementation returns `null`.
         *
         * @param oldItemPosition The position of the item in the old list
         * @param newItemPosition The position of the item in the new list
         *
         * @return A payload object that represents the change between the two items.
         */
        fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            return null
        }
    }

    /**
     * Callback for calculating the diff between two non-null items in a list.
     *
     *
     * [Callback] serves two roles - list indexing, and item diffing. ItemCallback handles
     * just the second of these, which allows separation of code that indexes into an array or List
     * from the presentation-layer and content specific diffing code.
     *
     * @param <T> Type of items to compare.
    </T> */
    abstract class ItemCallback<T>() {
        /**
         * Called to check whether two objects represent the same item.
         *
         *
         * For example, if your items have unique ids, this method should check their id equality.
         *
         *
         * Note: `null` items in the list are assumed to be the same as another `null`
         * item and are assumed to not be the same as a non-`null` item. This callback will
         * not be invoked for either of those cases.
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True if the two items represent the same object or false if they are different.
         *
         * @see Callback.areItemsTheSame
         */
        abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean

        /**
         * Called to check whether two items have the same data.
         *
         *
         * This information is used to detect if the contents of an item have changed.
         *
         *
         * This method to check equality instead of [Object.equals] so that you can
         * change its behavior depending on your UI.
         *
         *
         * For example, if you are using DiffUtil with a
         * [RecyclerView.Adapter], you should
         * return whether the items' visual representations are the same.
         *
         *
         * This method is called only if [.areItemsTheSame] returns `true` for
         * these items.
         *
         *
         * Note: Two `null` items are assumed to represent the same contents. This callback
         * will not be invoked for this case.
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True if the contents of the items are the same or false if they are different.
         *
         * @see Callback.areContentsTheSame
         */
        abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean

        /**
         * When [.areItemsTheSame] returns `true` for two items and
         * [.areContentsTheSame] returns false for them, this method is called to
         * get a payload about the change.
         *
         *
         * For example, if you are using DiffUtil with [RecyclerView], you can return the
         * particular field that changed in the item and your
         * [ItemAnimator][RecyclerView.ItemAnimator] can use that
         * information to run the correct animation.
         *
         *
         * Default implementation returns `null`.
         *
         * @see Callback.getChangePayload
         */
        fun getChangePayload(oldItem: T, newItem: T): Any? {
            return null
        }
    }

    /**
     * Snakes represent a match between two lists. It is optionally prefixed or postfixed with an
     * add or remove operation. See the Myers' paper for details.
     */
    class Snake() {
        /**
         * Position in the old list
         */
        var x = 0

        /**
         * Position in the new list
         */
        var y = 0

        /**
         * Number of matches. Might be 0.
         */
        var size = 0

        /**
         * If true, this is a removal from the original list followed by `size` matches.
         * If false, this is an addition from the new list followed by `size` matches.
         */
        var removal = false

        /**
         * If true, the addition or removal is at the end of the snake.
         * If false, the addition or removal is at the beginning of the snake.
         */
        var reverse = false
    }

    /**
     * Represents a range in two lists that needs to be solved.
     *
     *
     * This internal class is used when running Myers' algorithm without recursion.
     */
    internal class Range {
        var oldListStart = 0
        var oldListEnd = 0
        var newListStart = 0
        var newListEnd = 0

        constructor() {}
        constructor(oldListStart: Int, oldListEnd: Int, newListStart: Int, newListEnd: Int) {
            this.oldListStart = oldListStart
            this.oldListEnd = oldListEnd
            this.newListStart = newListStart
            this.newListEnd = newListEnd
        }
    }

    /**
     * This class holds the information about the result of a
     * [DiffUtil.calculateDiff] call.
     *
     *
     * You can consume the updates in a DiffResult via
     * [.dispatchUpdatesTo] or directly stream the results into a
     * [RecyclerView.Adapter] via [.dispatchUpdatesTo].
     */
    class DiffResult internal constructor(
        callback: Callback, // The Myers' snakes. At this point, we only care about their diagonal sections.
        val snakes: MutableList<Snake>, // The list to keep oldItemStatuses. As we traverse old items, we assign flags to them
        // which also includes whether they were a real removal or a move (and its new index).
        private var mOldItemStatuses: IntArray,
        newItemStatuses: IntArray, detectMoves: Boolean
    ) {

        // The list to keep newItemStatuses. As we traverse new items, we assign flags to them
        // which also includes whether they were a real addition or a move(and its old index).
        private val mNewItemStatuses: IntArray

        // The callback that was given to calcualte diff method.
        private val mCallback: Callback
        private val mOldListSize: Int
        private val mNewListSize: Int
        private val mDetectMoves: Boolean

        /**
         * We always add a Snake to 0/0 so that we can run loops from end to beginning and be done
         * when we run out of snakes.
         */
        private fun addRootSnake() {
            val firstSnake = if (snakes.isEmpty()) null else snakes[0]
            if ((firstSnake == null) || (firstSnake.x != 0) || (firstSnake.y != 0)) {
                val root = Snake()
                root.x = 0
                root.y = 0
                root.removal = false
                root.size = 0
                root.reverse = false
                snakes.add(0, root)
            }
        }

        /**
         * This method traverses each addition / removal and tries to match it to a previous
         * removal / addition. This is how we detect move operations.
         *
         *
         * This class also flags whether an item has been changed or not.
         *
         *
         * DiffUtil does this pre-processing so that if it is running on a big list, it can be moved
         * to background thread where most of the expensive stuff will be calculated and kept in
         * the statuses maps. DiffResult uses this pre-calculated information while dispatching
         * the updates (which is probably being called on the main thread).
         */
        private fun findMatchingItems() {
            var posOld = mOldListSize
            var posNew = mNewListSize
            // traverse the matrix from right bottom to 0,0.
            for (i in snakes.indices.reversed()) {
                val snake = snakes[i]
                val endX = snake.x + snake.size
                val endY = snake.y + snake.size
                if (mDetectMoves) {
                    while (posOld > endX) {
                        // this is a removal. Check remaining snakes to see if this was added before
                        findAddition(posOld, posNew, i)
                        posOld--
                    }
                    while (posNew > endY) {
                        // this is an addition. Check remaining snakes to see if this was removed
                        // before
                        findRemoval(posOld, posNew, i)
                        posNew--
                    }
                }
                for (j in 0 until snake.size) {
                    // matching items. Check if it is changed or not
                    val oldItemPos = snake.x + j
                    val newItemPos = snake.y + j
                    val theSame = mCallback
                        .areContentsTheSame(oldItemPos, newItemPos)
                    val changeFlag = if (theSame) FLAG_NOT_CHANGED else FLAG_CHANGED
                    mOldItemStatuses[oldItemPos] = (newItemPos shl FLAG_OFFSET) or changeFlag
                    mNewItemStatuses[newItemPos] = (oldItemPos shl FLAG_OFFSET) or changeFlag
                }
                posOld = snake.x
                posNew = snake.y
            }
        }

        private fun findAddition(x: Int, y: Int, snakeIndex: Int) {
            if (mOldItemStatuses[x - 1] != 0) {
                return  // already set by a latter item
            }
            findMatchingItem(x, y, snakeIndex, false)
        }

        private fun findRemoval(x: Int, y: Int, snakeIndex: Int) {
            if (mNewItemStatuses[y - 1] != 0) {
                return  // already set by a latter item
            }
            findMatchingItem(x, y, snakeIndex, true)
        }

        /**
         * Given a position in the old list, returns the position in the new list, or
         * `NO_POSITION` if it was removed.
         *
         * @param oldListPosition Position of item in old list
         *
         * @return Position of item in new list, or `NO_POSITION` if not present.
         *
         * @see .NO_POSITION
         *
         * @see .convertNewPositionToOld
         */
        fun convertOldPositionToNew(oldListPosition: Int): Int {
            if (oldListPosition < 0 || oldListPosition >= mOldListSize) {
                throw IndexOutOfBoundsException(
                    ("Index out of bounds - passed position = "
                            + oldListPosition + ", old list size = " + mOldListSize)
                )
            }
            val status = mOldItemStatuses[oldListPosition]
            return if ((status and FLAG_MASK) == 0) {
                NO_POSITION
            } else {
                status shr FLAG_OFFSET
            }
        }

        /**
         * Given a position in the new list, returns the position in the old list, or
         * `NO_POSITION` if it was removed.
         *
         * @param newListPosition Position of item in new list
         *
         * @return Position of item in old list, or `NO_POSITION` if not present.
         *
         * @see .NO_POSITION
         *
         * @see .convertOldPositionToNew
         */
        fun convertNewPositionToOld(newListPosition: Int): Int {
            if (newListPosition < 0 || newListPosition >= mNewListSize) {
                throw IndexOutOfBoundsException(
                    ("Index out of bounds - passed position = "
                            + newListPosition + ", new list size = " + mNewListSize)
                )
            }
            val status = mNewItemStatuses[newListPosition]
            return if ((status and FLAG_MASK) == 0) {
                NO_POSITION
            } else {
                status shr FLAG_OFFSET
            }
        }

        /**
         * Finds a matching item that is before the given coordinates in the matrix
         * (before : left and above).
         *
         * @param x The x position in the matrix (position in the old list)
         * @param y The y position in the matrix (position in the new list)
         * @param snakeIndex The current snake index
         * @param removal True if we are looking for a removal, false otherwise
         *
         * @return True if such item is found.
         */
        private fun findMatchingItem(
            x: Int, y: Int, snakeIndex: Int,
            removal: Boolean
        ): Boolean {
            val myItemPos: Int
            var curX: Int
            var curY: Int
            if (removal) {
                myItemPos = y - 1
                curX = x
                curY = y - 1
            } else {
                myItemPos = x - 1
                curX = x - 1
                curY = y
            }
            for (i in snakeIndex downTo 0) {
                val snake = snakes[i]
                val endX = snake.x + snake.size
                val endY = snake.y + snake.size
                if (removal) {
                    // check removals for a match
                    for (pos in curX - 1 downTo endX) {
                        if (mCallback.areItemsTheSame(pos, myItemPos)) {
                            // found!
                            val theSame = mCallback.areContentsTheSame(pos, myItemPos)
                            val changeFlag = if (theSame) FLAG_MOVED_NOT_CHANGED else FLAG_MOVED_CHANGED
                            mNewItemStatuses[myItemPos] = (pos shl FLAG_OFFSET) or FLAG_IGNORE
                            mOldItemStatuses[pos] = (myItemPos shl FLAG_OFFSET) or changeFlag
                            return true
                        }
                    }
                } else {
                    // check for additions for a match
                    for (pos in curY - 1 downTo endY) {
                        if (mCallback.areItemsTheSame(myItemPos, pos)) {
                            // found
                            val theSame = mCallback.areContentsTheSame(myItemPos, pos)
                            val changeFlag = if (theSame) FLAG_MOVED_NOT_CHANGED else FLAG_MOVED_CHANGED
                            mOldItemStatuses[x - 1] = (pos shl FLAG_OFFSET) or FLAG_IGNORE
                            mNewItemStatuses[pos] = ((x - 1) shl FLAG_OFFSET) or changeFlag
                            return true
                        }
                    }
                }
                curX = snake.x
                curY = snake.y
            }
            return false
        }

        /**
         * Dispatches the update events to the given adapter.
         *
         *
         * For example, if you have an [Adapter][RecyclerView.Adapter]
         * that is backed by a [List], you can swap the list with the new one then call this
         * method to dispatch all updates to the RecyclerView.
         * <pre>
         * List oldList = mAdapter.getData();
         * DiffResult result = DiffUtil.calculateDiff(new MyCallback(oldList, newList));
         * mAdapter.setData(newList);
         * result.dispatchUpdatesTo(mAdapter);
        </pre> *
         *
         *
         * Note that the RecyclerView requires you to dispatch adapter updates immediately when you
         * change the data (you cannot defer `notify*` calls). The usage above adheres to this
         * rule because updates are sent to the adapter right after the backing data is changed,
         * before RecyclerView tries to read it.
         *
         *
         * On the other hand, if you have another
         * [AdapterDataObserver][RecyclerView.AdapterDataObserver]
         * that tries to process events synchronously, this may confuse that observer because the
         * list is instantly moved to its final state while the adapter updates are dispatched later
         * on, one by one. If you have such an
         * [AdapterDataObserver][RecyclerView.AdapterDataObserver],
         * you can use
         * [.dispatchUpdatesTo] to handle each modification
         * manually.
         *
         * @param adapter A RecyclerView adapter which was displaying the old list and will start
         * displaying the new list.
         * @see AdapterListUpdateCallback
         */
        /*fun dispatchUpdatesTo(adapter: RecyclerView.Adapter) {
            dispatchUpdatesTo(AdapterListUpdateCallback(adapter))
        }*/

        /**
         * Dispatches update operations to the given Callback.
         *
         *
         * These updates are atomic such that the first update call affects every update call that
         * comes after it (the same as RecyclerView).
         *
         * @param updateCallback The callback to receive the update operations.
         * @see .dispatchUpdatesTo
         */
        fun dispatchUpdatesTo(updateCallback: ListUpdateCallback) {
            var updateCallback: ListUpdateCallback = updateCallback
            val batchingCallback: BatchingListUpdateCallback
            if (updateCallback is BatchingListUpdateCallback) {
                batchingCallback = updateCallback as BatchingListUpdateCallback
            } else {
                batchingCallback = BatchingListUpdateCallback(updateCallback)
                // replace updateCallback with a batching callback and override references to
                // updateCallback so that we don't call it directly by mistake
                updateCallback = batchingCallback
            }
            // These are add/remove ops that are converted to moves. We track their positions until
            // their respective update operations are processed.
            val postponedUpdates: MutableList<PostponedUpdate> = ArrayList()
            var posOld = mOldListSize
            var posNew = mNewListSize
            for (snakeIndex in snakes.indices.reversed()) {
                val snake = snakes[snakeIndex]
                val snakeSize = snake.size
                val endX = snake.x + snakeSize
                val endY = snake.y + snakeSize
                if (endX < posOld) {
                    dispatchRemovals(postponedUpdates, batchingCallback, endX, posOld - endX, endX)
                }
                if (endY < posNew) {
                    dispatchAdditions(
                        postponedUpdates, batchingCallback, endX, posNew - endY,
                        endY
                    )
                }
                for (i in snakeSize - 1 downTo 0) {
                    if ((mOldItemStatuses[snake.x + i] and FLAG_MASK) == FLAG_CHANGED) {
                        batchingCallback.onChanged(
                            snake.x + i, 1,
                            mCallback.getChangePayload(snake.x + i, snake.y + i)
                        )
                    }
                }
                posOld = snake.x
                posNew = snake.y
            }
            batchingCallback.dispatchLastEvent()
        }

        private fun dispatchAdditions(
            postponedUpdates: MutableList<PostponedUpdate>,
            updateCallback: ListUpdateCallback, start: Int, count: Int, globalIndex: Int
        ) {
            if (!mDetectMoves) {
                updateCallback.onInserted(start, count)
                return
            }
            for (i in count - 1 downTo 0) {
                val status = mNewItemStatuses[globalIndex + i] and FLAG_MASK
                when (status) {
                    0 -> {
                        updateCallback.onInserted(start, 1)
                        for (update: PostponedUpdate in postponedUpdates) {
                            update.currentPos += 1
                        }
                    }
                    FLAG_MOVED_CHANGED, FLAG_MOVED_NOT_CHANGED -> {
                        val pos = mNewItemStatuses[globalIndex + i] shr FLAG_OFFSET
                        val update = removePostponedUpdate(
                            postponedUpdates, pos,
                            true
                        )
                        // the item was moved from that position
                        updateCallback.onMoved(update!!.currentPos, start)
                        if (status == FLAG_MOVED_CHANGED) {
                            // also dispatch a change
                            updateCallback.onChanged(
                                start, 1,
                                mCallback.getChangePayload(pos, globalIndex + i)
                            )
                        }
                    }
                    FLAG_IGNORE -> postponedUpdates.add(PostponedUpdate(globalIndex + i, start, false))
                    else -> throw IllegalStateException(
                        "unknown flag for pos " + (globalIndex + i) + " " + java.lang.Long
                            .toBinaryString(status.toLong())
                    )
                }
            }
        }

        private fun dispatchRemovals(
            postponedUpdates: MutableList<PostponedUpdate>,
            updateCallback: ListUpdateCallback, start: Int, count: Int, globalIndex: Int
        ) {
            if (!mDetectMoves) {
                updateCallback.onRemoved(start, count)
                return
            }
            for (i in count - 1 downTo 0) {
                val status = mOldItemStatuses[globalIndex + i] and FLAG_MASK
                when (status) {
                    0 -> {
                        updateCallback.onRemoved(start + i, 1)
                        for (update: PostponedUpdate in postponedUpdates) {
                            update.currentPos -= 1
                        }
                    }
                    FLAG_MOVED_CHANGED, FLAG_MOVED_NOT_CHANGED -> {
                        val pos = mOldItemStatuses[globalIndex + i] shr FLAG_OFFSET
                        val update = removePostponedUpdate(
                            postponedUpdates, pos,
                            false
                        )
                        // the item was moved to that position. we do -1 because this is a move not
                        // add and removing current item offsets the target move by 1
                        updateCallback.onMoved(start + i, update!!.currentPos - 1)
                        if (status == FLAG_MOVED_CHANGED) {
                            // also dispatch a change
                            updateCallback.onChanged(
                                update.currentPos - 1, 1,
                                mCallback.getChangePayload(globalIndex + i, pos)
                            )
                        }
                    }
                    FLAG_IGNORE -> postponedUpdates.add(PostponedUpdate(globalIndex + i, start + i, true))
                    else -> throw IllegalStateException(
                        "unknown flag for pos " + (globalIndex + i) + " " + java.lang.Long
                            .toBinaryString(status.toLong())
                    )
                }
            }
        }

        companion object {
            /**
             * Signifies an item not present in the list.
             */
            val NO_POSITION = -1

            /**
             * While reading the flags below, keep in mind that when multiple items move in a list,
             * Myers's may pick any of them as the anchor item and consider that one NOT_CHANGED while
             * picking others as additions and removals. This is completely fine as we later detect
             * all moves.
             *
             *
             * Below, when an item is mentioned to stay in the same "location", it means we won't
             * dispatch a move/add/remove for it, it DOES NOT mean the item is still in the same
             * position.
             */
            // item stayed the same.
            private val FLAG_NOT_CHANGED = 1

            // item stayed in the same location but changed.
            private val FLAG_CHANGED = FLAG_NOT_CHANGED shl 1

            // Item has moved and also changed.
            private val FLAG_MOVED_CHANGED = FLAG_CHANGED shl 1

            // Item has moved but did not change.
            private val FLAG_MOVED_NOT_CHANGED = FLAG_MOVED_CHANGED shl 1

            // Ignore this update.
            // If this is an addition from the new list, it means the item is actually removed from an
            // earlier position and its move will be dispatched when we process the matching removal
            // from the old list.
            // If this is a removal from the old list, it means the item is actually added back to an
            // earlier index in the new list and we'll dispatch its move when we are processing that
            // addition.
            private val FLAG_IGNORE = FLAG_MOVED_NOT_CHANGED shl 1

            // since we are re-using the int arrays that were created in the Myers' step, we mask
            // change flags
            private val FLAG_OFFSET = 5
            private val FLAG_MASK = (1 shl FLAG_OFFSET) - 1
            private fun removePostponedUpdate(
                updates: MutableList<PostponedUpdate>,
                pos: Int, removal: Boolean
            ): PostponedUpdate? {
                for (i in updates.indices.reversed()) {
                    val update = updates[i]
                    if (update.posInOwnerList == pos && update.removal == removal) {
                        updates.removeAt(i)
                        for (j in i until updates.size) {
                            // offset other ops since they swapped positions
                            updates[j].currentPos += if (removal) 1 else -1
                        }
                        return update
                    }
                }
                return null
            }
        }

        /**
         * @param callback The callback that was used to calculate the diff
         * @param snakes The list of Myers' snakes
         * @param oldItemStatuses An int[] that can be re-purposed to keep metadata
         * @param newItemStatuses An int[] that can be re-purposed to keep metadata
         * @param detectMoves True if this DiffResult will try to detect moved items
         */
        init {
            mOldItemStatuses = mOldItemStatuses
            mNewItemStatuses = newItemStatuses
            Arrays.fill(mOldItemStatuses, 0)
            Arrays.fill(mNewItemStatuses, 0)
            mCallback = callback
            mOldListSize = callback.oldListSize
            mNewListSize = callback.newListSize
            mDetectMoves = detectMoves
            addRootSnake()
            findMatchingItems()
        }
    }

    /**
     * Represents an update that we skipped because it was a move.
     *
     *
     * When an update is skipped, it is tracked as other updates are dispatched until the matching
     * add/remove operation is found at which point the tracked position is used to dispatch the
     * update.
     */
    private class PostponedUpdate(posInOwnerList: Int, currentPos: Int, removal: Boolean) {
        var posInOwnerList: Int
        var currentPos: Int
        var removal: Boolean

        init {
            this.posInOwnerList = posInOwnerList
            this.currentPos = currentPos
            this.removal = removal
        }
    }
}