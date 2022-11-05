import java.util.Comparator
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

object SortingDelegate {

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
     * Wrapper to {@link sort(MutableList<T>,List<KProperty1<T, Comparable<Any>?>>,List<Boolean>)} using [Map] for a more convenient API
     * */
    inline fun <reified T> sort(
        workingList: MutableList<T>,
        sortCriteriaMappedToAscending: Map<KProperty1<T, Comparable<Any>?>, Boolean>
    ) = sort(workingList, sortCriteriaMappedToAscending.keys.toList(), sortCriteriaMappedToAscending.values.toList())

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

}