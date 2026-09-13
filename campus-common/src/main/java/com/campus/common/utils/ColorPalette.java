package com.campus.common.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 色块调色板（替代商品图片/头像，前后端共享色板约定）
 */
public class ColorPalette {

    /** 校园清新风预置色板 */
    public static final String[] COLORS = {
            "#FF7675", "#74B9FF", "#55EFC4", "#A29BFE", "#FAB1A0",
            "#FFEAA7", "#81ECEC", "#DFE6E9", "#FDCB6E", "#6C5CE7"
    };

    /** 随机取一个色板颜色 */
    public static String random() {
        return COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)];
    }

    /** 校验是否为合法色值，非法则返回随机色 */
    public static String normalize(String color) {
        return RegexUtils.isColor(color) ? color.toUpperCase() : random();
    }
}
