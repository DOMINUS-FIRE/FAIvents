package me.dominus.faivents.util;

import java.util.List;
import java.util.Random;

public final class RandUtil {

    private static final Random RANDOM = new Random();

    private RandUtil() {
    }

    public static int nextInt(int min, int max) {
        if (max <= min) {
            return min;
        }
        return RANDOM.nextInt((max - min) + 1) + min;
    }

    public static boolean chance(double chance) {
        return RANDOM.nextDouble() <= chance;
    }

    public static <T> T pick(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(RANDOM.nextInt(list.size()));
    }
}

