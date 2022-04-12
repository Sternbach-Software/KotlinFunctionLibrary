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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.system.measureNanoTime


/**
 * Library of Kotlin utility functions. Version: 3.0.0
 * */
@Suppress("UNUSED")
object KotlinFunctionLibrary {
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
    val randomWord = { wordLength: Int ->
        (1..wordLength)
            .map { (('A'..'Z') + ('a'..'z')).random() }
            .joinToString("")
    }

    /**
     * Lambda to generate a list of size [numWords] containing random words of size [wordLength]
     * */
    val listOfRandomWords = { numWords: Int, wordLength: Int ->
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
        message: String = if (inSeconds) "Time to complete: %f seconds" else "Time to complete: %d nanoseconds",
        block: () -> Unit
    ) {
        val timeToComplete = measureNanoTime(block)
        System.out.printf(message, if (inSeconds) timeToComplete / 1_000_000_000.00 else timeToComplete)
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
        message: String = if (inSeconds) "Time to complete: %f seconds" else "Time to complete: %d nanoseconds",
        block: () -> T
    ): T {
        var result: T
        val timeToComplete = measureNanoTime { result = block() }
        System.out.printf(message, if (inSeconds) timeToComplete / 1_000_000_000.00 else timeToComplete)
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
     * Takes a [Triple] of <hour,minute,second> and returns either e.g. "05:32:15", or "5 hr 32 min 15 sec".
     * Valid outputs: 12:34, 00:12, 00:00, 1:00:00, 1:12:00
     * Invalid outputs: "00:00:00", "00:01:00", "1:5:3"
     * */
    fun Triple<Int, Int, Int>.formatted(withColons: Boolean): String {
        fun Int.formatted() = when {
            this == 0 -> "00"
            this < 10 -> "0$this"
            else -> this.toString()
        }
        return if (withColons) {
            val string = when {
                first == 0 && second > 0 -> "$second:"
                first > 0 -> "$first:${second.formatted()}:"
                second == 0 -> "00:"
                else -> TODO("Should not have happened. this=$this") //how beautiful! Also deals with first == 0 && second == 0
            }
            string + third.formatted()
        } else timeFormattedConcisely(first, second, third)
    }

    /**
     * Takes an hour, minute, and second, and will return a string with only those values which are not equal to 0 (e.g. "5 hr 15 sec", "5 hr 32 min 15 sec")
     * */
    fun timeFormattedConcisely(hour: Int, minute: Int, second: Int): String {
        val string = StringBuilder()
        if (hour != 0) string.append("$hour hr ")
        if (minute != 0) string.append("$minute min ")
        if (second != 0) string.append("$second sec")
        return if (string.isEmpty()) "0 sec" else string.toString().trim()
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

        val size = requireNotEmptyAndSameSize(
            ascending, sortCriteria, "List of ascending/descending must not be empty",
            "List of sort criteria must not be empty",
            "Each sort criteria must be matched with a ascending/descending boolean; size of ascending/descending list: %d, Size of sort criteria list: %d "
        )

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
        val size = requireNotEmptyAndSameSize(
            ascending, sortCriteria, "List of ascending/descending must not be empty",
            "List of sort criteria must not be empty",
            "Each sort criteria must be matched with a ascending/descending boolean; size of ascending/descending list: %d, Size of sort criteria list: %d "
        )
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
        sortCriteriaMappedToAscending: Map<KProperty1<T, Comparable<Any>?>, Boolean>
    ) = sort(workingList, sortCriteriaMappedToAscending.keys.toList(), sortCriteriaMappedToAscending.values.toList())

    /**
     * Wrapper to {@link sort(MutableList<T>,List<String>,List<Boolean>)} using [Map] for a more convenient API
     * */
    @JvmName("sortWithClassParameterStrings")
    inline fun <reified T> sort(
        workingList: MutableList<T>,
        sortCriteriaMappedToAscending: Map<String, Boolean>
    ) = sort(
        workingList,
        sortCriteriaMappedToAscending.keys.toMutableList(),
        sortCriteriaMappedToAscending.values.toList()
    )

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
    ): Pair<Int, Int> {
        val sizeA = listA.size
        val sizeB = listB.size
        require(sizeA > 0) { System.out.printf(aEmptyMessage, sizeA) }
        require(sizeB > 0) { System.out.printf(bEmptyMessage, sizeB) }
        require(sizeB == sizeA) { System.out.printf(notSameSizeMessage, sizeA, sizeB) }
        return Pair(sizeA, sizeB)
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
    @PublishedApi
    internal inline fun <reified T> PRIVATEgetComparator(
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
    @PublishedApi
    internal fun <T> PRIVATEgetComparator(
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
    @PublishedApi
    internal inline fun <reified T> PRIVATEgetPropertyToSortBy(
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
            if (isDirectory) deleteRecursively().println("${this.name} deleted recursively.")
            else delete().println("${this.name} deleted.")
        }
        createNewFile()
        return this //to allow for functional programming/ chaining calls
    }

    /**
     * Creates a folder if non-existant, or overwrites it if [overwrite] is true
     * */
    fun File.createFolder(createParentFolders: Boolean = false, overwrite: Boolean = false): File {
        if (overwrite && exists()) deleteRecursively()
        if (createParentFolders) mkdirs() else mkdir()
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
        val r: R
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
        return if (predicate(this)) this + append else this
    }

    /**
     * Appends [append] to [this] if [predicate] returns true when passed [this], otherwise appends [else]
     * Use case:                 it.appendLine("High-risk demographic: ".appendIf("Male(${first.ageRange.first}-${first.ageRange.last})","N\\A") { demographics.size == 1 }.appendIf("Female(${first.ageRange.first}-${first.ageRange.last}") { demographics.size == 2 } )

     * */
    fun String.appendIf(append: String, `else`: String, predicate: (String) -> Boolean): String {
        return if (predicate(this)) this + append else this + `else`
    }

    fun <T> myBuildList(capacity: Int, action: () -> T): List<T> {
        val list = mutableListOf<T>()
        for (i in 0 until capacity) list.add(action())
        return list.toList()
    }

    fun <T> myBuildMutableList(capacity: Int, action: () -> T): MutableList<T> {
        val list = mutableListOf<T>()
        for (i in 0 until capacity) list.add(action())
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
        val endOfStr1 = indexOf(str1) + str1.length
        return substring(endOfStr1, indexOf(str2, endOfStr1))
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
    fun String.substringBetween(
        str1: String,
        str2: String,
        returnIndicesFromStartOfString: Boolean
    ): Triple<String, Int, Int> {
        require(this.isNotBlank()) { "The string passed to subStringBetween as `this` was empty. params: \"$this\".substringBetween(\"$str1\", \"$str2\", \"$returnIndicesFromStartOfString\")" }
        val index1 = indexOf(str1)
        if (index1 < 0) throw StringIndexOutOfBoundsException("String 1 (\"$str1\") was not found in \"$this\".")
        val index2 = indexOf(str2, index1)
        if (index2 < 0) throw StringIndexOutOfBoundsException("String 2 (\"$str2\") was not found in \"$this\".")
        val indexOfEndOfFirstWord = index1 + str1.length
//        println("this=$this,str1=$str1,str2=$str2")
//        println("index1=$index1,index2=$index2")
        val str = substring(indexOfEndOfFirstWord, index2)
        return Triple(
            str,
            if (returnIndicesFromStartOfString) index1 else indexOfEndOfFirstWord,
            if (returnIndicesFromStartOfString) index2 else index2 + str2.length
        )
    }

    /**
     * Returns the substring between [startIndex] and the next occurence of [endString] after [startIndex]
     * @return [Pair]<abovementioned substring, index of [endString]>
     * */
    fun String.substring(startIndex: Int, endString: String): Pair<String, Int> {
        val endIndex = indexOf(endString, startIndex)
        return Pair(substring(startIndex, endIndex), endIndex)
    }

    /**
     * Returns the substring between [startIndex] and the next occurence of [endString] after [startIndex]
     * @return [Pair]<abovementioned substring, index of [endString]>
     * */
    fun String.substring(
        startIndex: Int,
        endString: String,
        returnIndexFromStartOfFoundString: Boolean
    ): Pair<String, Int> {
        val endIndex = indexOf(endString, startIndex)
        return Pair(
            substring(startIndex, endIndex),
            if (returnIndexFromStartOfFoundString) endIndex else endIndex + endString.length
        )
    }

    fun toLookBehindMatchAhead(behind: String, match: String, ahead: String): Regex {
        return "(?<=$behind)$match(?=$ahead)".toRegex()
    }

    /**
     * Takes a string of the form \"look_behind~~match~~look_ahead\" and returns a [Regex] with that form (after removing the ~~)
     * */
    fun String.toLookBehindMatchAhead(escapeIllegalCharacters: Boolean = true): Regex {
        require(count { it == '~' } == 4)
        var (behind, match, ahead) = split("~~")
        fun String.escapeIllegalCharacters(): String {
            var temp = this
            fun replaceIfContains(vararg strings: String) {
                for (string in strings) if (temp.contains(string)) temp = temp.replace(string, "\\$string")
            }
            replaceIfContains("(", ")", "[", "]", "{", "}")
            return temp
        }
        if (escapeIllegalCharacters) {
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
        val biggerString = if (str1.length > str2.length) str1 else str2
        val smallerString = if (biggerString === str1) str2 else str1
        val missingIndices = getDiff(biggerString, smallerString)
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
            if (this[i] == element) return i
        }
        return null
    }

    fun <T> Iterable<T>.toFrequencyMap(): Map<T, Int> {
        val frequencies: MutableMap<T, Int> = mutableMapOf()
        this.forEach { frequencies[it] = frequencies.getOrDefault(it, 0) + 1 }
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

    /**
     * A version of List.find() for recursive data structures. Recurses through a list of a recursive elements to find the element which matches [predicate] by selecting the next list using [recursiveSelector]
     * For example, given class Foo(val a: Char, val b: List<Foo>)
     * val list = listOf(
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
    list.recursiveFind({ it.a == 'z' }) { it.b } != null == false
    list.recursiveFind({ it.a == 'a' }) { it.b } != null == true
    list.recursiveFind({ it.a == 'f' }) { it.b } != null == true
     * */
    fun <E> List<E>.recursiveFind(predicate: (E) -> Boolean, recursiveSelector: (E) -> List<E>): E? {
        if (isEmpty()) return null
        for (element in this) {
            return if (predicate(element)) element else /*check the next layer*/ recursiveSelector(element).recursiveFind(
                predicate,
                recursiveSelector
            ) ?: continue
        }
        return null
    }
    fun <E> List<E>.recursiveForEach(action: (E) -> Unit, recursiveSelector: (E) -> List<E>): Unit {
        if (isEmpty()) return
        for (element in this) {
            action(element) 
            recursiveSelector(element).recursiveForEach(
                action,
                recursiveSelector
            )
      }
    }
/**
* Determines whether [this] list contains a sublist such that at least one element in each list of said sublist is contained in a parallel sublist of [other].
* This is a direct adaptation of [CharSequence.contains(CharSequence)].
* For example the following returns true:
val x = listOf(                       listOf('a', 'b', 'c'), listOf('d', 'e', 'f')             )
val y = listOf(listOf('x' ,'y' ,'z'), listOf('1', '2', 'a'), listOf('3', 'f', '4'), listOf('9'))
val z = listOf(                       listOf('1', '2', 'a'), listOf('3', 'f', '4')             )
println(x in y) //prints true
println(x in z) //prints true
*/
    operator fun <T> List<Iterable<T>>.contains(other: List<Iterable<T>>): Boolean = contains(other) { thisList, otherList -> thisList.any { it in otherList } }
        
    fun <T> List<T>.contains(other: List<T>, predicate: (thisElement: T, otherElement: T) -> Boolean): Boolean = indexOf(other, 0, this.size, predicate) >= 0

    private fun <T> List<T>.indexOf(other: List<T>, startIndex: Int, endIndex: Int, predicate: (thisElement: T, otherElement: T) -> Boolean): Int {
        fun <T> List<T>.regionMatches(thisOffset: Int, other: List<T>, otherOffset: Int, size: Int, predicate: (thisElement: T, otherElement: T) -> Boolean): Boolean {
            if ((otherOffset < 0) || (thisOffset < 0) || (thisOffset > this.size - size) || (otherOffset > other.size - size)) {
                return false
            }

            for (index in 0 until size) {
                if (!predicate(this[thisOffset + index], other[otherOffset + index]))
                    return false
            }
            return true
        }
        val indices = startIndex.coerceAtLeast(0)..endIndex.coerceAtMost(this.size)

        for (index in indices) {
            if (other.regionMatches(0, this, index, other.size, predicate))
                return index
        }
        return -1
    }
    
    
    private fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int = if (this is Collection<*>) this.size else default
    
    fun <E, R> Iterable<E>.recursiveMap(
        transform: (E) -> R,
        recursiveSelector: (E) -> Iterable<E>
    ): List<R> = recursiveMapTo(ArrayList(collectionSizeOrDefault(10)), transform, recursiveSelector)

    fun <T, R, C: MutableCollection<in R>> Iterable<T>.recursiveMapTo(
        destination: C,
        transform: (T) -> R,
        recursiveSelector: (T) -> Iterable<T>
    ): C {
        for (element in this) {
            destination.add(transform(element))
            recursiveSelector(element).recursiveMapTo(destination, transform, recursiveSelector) //transform children
        }
        return destination
    }

    /**
     * A `val foo by lazy {}` alternative that supports vars, viz. `var foo = by LazyMutable {}`
     * */
    class LazyMutable<T>(val initializer: () -> T) : ReadWriteProperty<Any?, T> {
        private val UNINITIALIZED_VALUE = Any()
        private var prop: Any? = UNINITIALIZED_VALUE

        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return if (prop == UNINITIALIZED_VALUE) {
                synchronized(this) {
                    return if (prop == UNINITIALIZED_VALUE) initializer().also { prop = it } else prop as T
                }
            } else prop as T
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            synchronized(this) {
                prop = value
            }
        }
    }
    
    
    /**
     * An iterable that mimics [withIndex], but replace the indices with the values of a second iterator.
     * This can be thought of as a lazy version of [Iterable.zip].
     * Iteration will only proceed so long as both iterators have more items, lending to the idea of "zipping" the iterators.
     * 
     * @param offset the number of elements to start iterating from. Must be less than the smallest one of the iterators. Assumes that the [Iterable]'s implementation of [Iterable.next] moves the iteration window/index forward, such that a subsequent call to [Iterable.next] will yield the next element in the [Iterable].
     * */
    class ZippingIterableWithOffset<A, B>(
        private val iterator1: Iterator<A>,
        private val iterator2: Iterator<B>,
        private val offset: Int = 0,
        private val applyOffsetToFirstIterator: Boolean,
        private val applyOffsetToSecondIterator: Boolean,
    ) : Iterable<Pair<A, B>> {
        init {
            require(offset >= 0)
            var index = 0
            while(index < offset) {
                if(applyOffsetToFirstIterator) iterator1.next()
                if(applyOffsetToSecondIterator) iterator2.next()
                index++
            }
        }
        override fun iterator(): Iterator<Pair<A, B>> {
            return object : Iterator<Pair<A, B>> {

                override fun hasNext(): Boolean {
                    return iterator1.hasNext() && iterator2.hasNext()
                }

                override fun next(): Pair<A, B> {
                    return iterator1.next() to iterator2.next()
                }
            }
        }
    }
    class ZippingIterable<A, B>(
        private val iterator1: Iterator<A>,
        private val iterator2: Iterator<B>,
    ) : Iterable<Pair<A, B>> {
        override fun iterator(): Iterator<Pair<A, B>> {
            return object : Iterator<Pair<A, B>> {

                override fun hasNext(): Boolean {
                    return iterator1.hasNext() && iterator2.hasNext()
                }

                override fun next(): Pair<A, B> {
                    return iterator1.next() to iterator2.next()
                }
            }
        }
    }

    fun <A, B> Iterable<A>.with(other: Iterable<B>): ZippingIterable<A, B> =
        ZippingIterable(this.iterator(), other.iterator())
    fun <A, B> Iterable<A>.withOffset(other: Iterable<B>, offset: Int, applyOffsetToFirstIterable: Boolean = true, applyOffsetToSecondIterable: Boolean = true): ZippingIterableWithOffset<A, B> =
        ZippingIterableWithOffset(this.iterator(), other.iterator(), offset, applyOffsetToFirstIterable,  applyOffsetToSecondIterable)

    @JvmStatic
    fun main(args: Array<String>) {
        println("KotlinFunctionLibrary v4.0.0")
    }
}
