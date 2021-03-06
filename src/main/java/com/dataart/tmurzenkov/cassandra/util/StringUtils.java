package com.dataart.tmurzenkov.cassandra.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * Why the heck this class was not added in JDK???
 *
 * @author tmurzenkov
 */
public interface StringUtils {
    /**
     * Is empty? True if it is.
     *
     * @param givenString {@link String}
     * @return true if it is.
     */
    static boolean isEmpty(String givenString) {
        return (null == givenString) || "".equals(givenString);
    }

    /**
     * Assembles list to {@link String} with delimiter ', '.
     *
     * @param list {@link List}
     * @param <T>  any instance
     * @return {@link String}
     */
    static <T> String makeString(List<T> list) {
        return (null == list || list.isEmpty()) ? "" : list.stream().filter(Objects::nonNull).map(Object::toString).collect(joining(", "));
    }

    /**
     * Assembles set to {@link String} with delimiter ', '.
     *
     * @param set {@link Set}
     * @param <T>  any instance
     * @return {@link String}
     */
    static <T> String makeString(Set<T> set) {
        return (null == set || set.isEmpty()) ? "" : set.stream().filter(Objects::nonNull).map(Object::toString).collect(joining(", "));
    }
}
