package com.wmp.downloader.test;

interface TestLambda {
    int test(int a, int b);
}

public class Test {
    static void main() {
        TestLambda testLambda = (int a, int b) -> {
            return 0;
        };

        TestLambda testLambda2 = (a, b) -> {
            return 0;
        };

        TestLambda testLambda3 = (int a, int b) -> 0;
    }
}