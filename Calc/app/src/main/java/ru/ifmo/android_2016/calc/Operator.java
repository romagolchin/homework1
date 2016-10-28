package ru.ifmo.android_2016.calc;

/**
 * Created by Roman on 27/10/2016.
 */

public enum Operator {
    MUL(1),
    DIV(1),
    ADD(2),
    SUB(2);

    public int precedence;

    Operator(int precedence) {
        this.precedence = precedence;
    }
}
