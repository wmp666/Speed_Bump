package com.wmp.downloader.test;

public class Test {
    static void main() {
        TestLambda testLambda = (int a, int b)-> {
                return 0;
        };

        TestLambda testLambda2 = (a, b)-> {
            return 0;
        };

        TestLambda testLambda3 = (int a, int b)-> 0;
    }
}

interface TestLambda{
    int test(int a, int b);
}