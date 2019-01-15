package com.ft.net.nio;

import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class MyTest {
    @Test
    public void testClearSet() {
        Set<Integer> si = new HashSet<>();
        si.add(0xfffffff1);
        si.add(4);
        si.add(0xfffffff3);
        si.add(0xfffffff2);

        // SelectionKey可以使用先遍历再清空的做法
        for (Integer it : si) {
            System.out.println(it);
            //si.remove(it); // set在修改后不能再继续遍历 java.util.ConcurrentModificationException
        }
        si.clear();
    }

    @Test
    public void testSet() {
        // hashset不保证有序
        Set<String> set = new HashSet<>();
        set.add("String1");
        set.add("String4");
        set.add("String3");
        set.add("String2");
        set.add("String5");
        set.forEach(e-> System.out.print(e+" "));
        System.out.println();


        //LinkedHashSet会保证元素的添加顺序
        Set<String> set2 = new LinkedHashSet<>();
        set2.add("String1");
        set2.add("String5");
        set2.add("String3");
        set2.add("String4");
        set2.add("String2");
        set2.forEach(e-> System.out.print(e+" "));
        System.out.println();


        //TreeSet保证元素自然顺序
        Set<String> set3 = new TreeSet<>();
        set3.add("String1");
        set3.add("String5");
        set3.add("String4");
        set3.add("String2");
        set3.add("String3");
        set3.forEach(e-> System.out.print(e+" "));
    }
}
