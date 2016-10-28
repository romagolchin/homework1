package ru.ifmo.android_2016.calc;

/**
 * Created by Roman on 22/10/2016.
 */
public enum State {
    // current operator wipes out the previous one; it means that there's no right operand yet
    OPERATOR,
    NUMBER,
    // after tapping '=' and before any sensible actions
    FINAL_RESULT,
    ERROR,
    NONE
}
