package ru.ifmo.android_2016.calc;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;


public final class CalculatorActivity extends AppCompatActivity {

    private static final int MAX_MAIN_LENGTH = 15;
    private static final int MAX_INTERMEDIATE_LENGTH = 20;
    private Character decimalSeparator;
    private String initialValue;
    private String errorMessage;

    @Nullable
    private Character pendingOperator;
    private TextView mainDisplay;
    private TextView intermediateDisplay;
    private Double result = 0.0;
    @Nullable
    private Double rightOperand;
    // true when last operator is +/-
    private boolean lowPriority;
    // true when current number contains separator (to prevent multiple separators)
    private boolean isFractional;
    // add parentheses when current operator has higher priority then the previous one
    private boolean addParen;
    State state;

    private StringBuilder numberBuilder;

    private final String STATE = "state";
    private final String LOW_PRIORITY = "lowPriority";
    private final String IS_FRACTIONAL = "isFractional";
    private final String ADD_PAREN = "addParen";
    private final String RESULT = "result";
    private final String PENDING_OPERATOR = "pendingOperator";
    private final String RIGHT_OPERAND = "rightOperand";
    private final String MAIN_TEXT = "mainText";
    private final String INTERMEDIATE_TEXT = "intermediateText";
    private final String NUMBER_BUILDER = "numberBuilder";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE, state);
        outState.putBoolean(LOW_PRIORITY, lowPriority);
        outState.putBoolean(IS_FRACTIONAL, isFractional);
        outState.putBoolean(ADD_PAREN, addParen);
        outState.putDouble(RESULT, result);
        if (pendingOperator != null)
            outState.putChar(PENDING_OPERATOR, pendingOperator);
        if (rightOperand != null)
            outState.putDouble(RIGHT_OPERAND, rightOperand);
        outState.putString(MAIN_TEXT, mainDisplay.getText().toString());
        outState.putString(INTERMEDIATE_TEXT, intermediateDisplay.getText().toString());
        outState.putString(NUMBER_BUILDER, numberBuilder.toString());
    }

    private Operator determineOperator(char c) {
        switch (c) {
            case '+':
                return Operator.ADD;
            case '-':
                return Operator.SUB;
            case '×':
                return Operator.MUL;
            default:
                return Operator.DIV;
        }
    }

    private void updateNumber() {
        String trimmed = trimNumber(mainDisplay.getText().toString());
        mainDisplay.setText(trimmed);
        numberBuilder.setLength(0);
        numberBuilder.append(trimmed);
    }

    private void updateDisplay() {
        String toTrim = numberBuilder.toString();
        numberBuilder.setLength(0);
        numberBuilder.append(trimNumber(toTrim));
        mainDisplay.setText(numberBuilder);
    }


    //invoked on start, after tapping C or =
    private void reset() {
        if (state != State.ERROR)
            state = State.FINAL_RESULT;
        addParen = lowPriority = isFractional = false;
        pendingOperator = null;
        numberBuilder.setLength(0);
    }

    String shortenDouble(Double x, int givenLen) {
        String preliminary = String.valueOf(x);
        if (preliminary.length() > givenLen) {
            int indexExp = preliminary.indexOf('E');
            boolean isScientific = indexExp != -1;
            if (indexExp == -1)
                indexExp = preliminary.length();
            int indexSep = preliminary.indexOf('.');
            int fractionalPartSize = indexExp - indexSep - 1;
            StringBuilder format = new StringBuilder("#.");
            for (int i = 0; i < fractionalPartSize - preliminary.length() + givenLen; ++i)
                format.append('#');
            // if number is represented in ordinary decimal format => it's between 1e-3 and 1e7
            // => integer part fits in the main display
            if (isScientific) {
                //scientific format: integer part (1 character) and exponent fit in the display
                // => have to shorten only fractional part in both cases
                format.append("E0");
            }
            DecimalFormat df = new DecimalFormat(format.toString());
            Log.d("preliminary", preliminary);
            Log.d("format", format.toString());
            Log.d("fractionalPartSize", String.valueOf(fractionalPartSize));
            return df.format(x);
        } else {
            return preliminary;
        }
    }

    private Double readNumber() {
        Double res;
        try {
            res = Double.parseDouble(numberBuilder.toString());
        } catch (NumberFormatException e) {
            res = 0.;
        }
        return res;
    }

    private String trimNumber(final CharSequence cs) {
        int index = cs.length();
        int i = (cs.toString()).indexOf('E');
        int sepInd = (cs.toString()).indexOf(decimalSeparator);
        if (i != -1) index = i;
        CharSequence preliminary = cs.subSequence(0, index);
        CharSequence exponent = (index == cs.length()) ? "" : cs.subSequence(index, cs.length());
        if (sepInd != -1) {
            // remove trailing zeros
            while (index >= 1 && cs.charAt(index - 1) == '0') {
                preliminary = cs.subSequence(0, index - 1);
                --index;
            }
            // remove unnecessary separator
            if (index >= 1 && cs.charAt(index - 1) == decimalSeparator) {
                preliminary = cs.subSequence(0, index - 1);
                isFractional = false;
            }
        }
        if (preliminary.equals("-0") || preliminary.equals("-"))
            preliminary = "0";
        return preliminary.toString() + exponent;
    }


    private void calculate() {
        Log.d("calculate", String.valueOf(result) + " " + String.valueOf((rightOperand)));
        if (rightOperand == null || pendingOperator == null) {
            return;
        }
        if (pendingOperator == '+') {
            result += rightOperand;
        } else if (pendingOperator == '-') {
            result -= rightOperand;
        } else if (pendingOperator == '×') {
            result *= rightOperand;
        } else if (pendingOperator == '÷') {
            result /= rightOperand;
        }
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            intermediateDisplay.setText("");
            mainDisplay.setText(errorMessage);
            state = State.ERROR;
            result = 0.0;
        }
        if (state != State.ERROR) {
            mainDisplay.setText(trimNumber(shortenDouble(result, MAX_MAIN_LENGTH)));
        }

    }

    public void onDigit(View view) {
        char digit = ((Button) view).getText().toString().charAt(0);
        int mainLength = mainDisplay.getText().length();
        //several zeros at start are not allowed
        if (state == State.NUMBER && mainLength >= MAX_MAIN_LENGTH)
            return;
        if (state != State.NUMBER) {
            numberBuilder.setLength(0);
            numberBuilder.append(digit);
            state = State.NUMBER;
        } else {
            if (!numberBuilder.toString().equals("0"))
                numberBuilder.append(digit);
            else if (digit != '0') {
                numberBuilder.setLength(0);
                numberBuilder.append(digit);
            } else {
                return;
            }
        }
        mainDisplay.setText(numberBuilder);
    }


    public void onDelete(View view) {
        int mainLength = mainDisplay.getText().length();
        if (state == State.NUMBER) {
            if (mainLength > 0 && mainDisplay.getText().charAt(mainLength - 1) == decimalSeparator)
                isFractional = false;
            if (mainLength == 1) {
                mainDisplay.setText(initialValue);
            } else {
                mainDisplay.setText(mainDisplay.getText().subSequence(0, mainLength - 1));
            }
        }
        updateNumber();
    }

    public void onEquals(View view) {
        if (state != State.ERROR) {
            if (numberBuilder.charAt(numberBuilder.length() - 1) == decimalSeparator) {
                mainDisplay.setText(numberBuilder.subSequence(0, numberBuilder.length() - 1));
            }
            if (pendingOperator != null) {
                if (state != State.OPERATOR) {
                    rightOperand = readNumber();
                } else {
                    return;
                }
                calculate();
            } else if (state == State.NUMBER) {
                result = readNumber();
            }
            intermediateDisplay.setText("");
            reset();
            Log.d("result", String.valueOf(result));

        }
    }

    public void onSeparator(View view) {
        int mainLength = mainDisplay.getText().length();
        if (state == State.NUMBER && mainLength >= MAX_MAIN_LENGTH)
            return;
        if (state == State.NUMBER && !isFractional) {
            numberBuilder.append(decimalSeparator);
            isFractional = true;
        }
        //omitting zero is allowed
        if (state != State.NUMBER) {
            numberBuilder.setLength(0);
            numberBuilder.append("0").append(decimalSeparator);
            state = State.NUMBER;
            isFractional = true;
        }
        mainDisplay.setText(numberBuilder);
    }

    public void onBinOperator(View view) {
        char operatorRepr = ((Button) view).getText().toString().charAt(0);
        Operator operator = determineOperator(operatorRepr);
        int intermediateLength = intermediateDisplay.getText().length();
        //the following operations are expected to increase the length of text in one or both displays
        //if length of main display is exceeded calculator waits for command other than digit or separator
        //if length of intermediate display is exceeded result is copied from main display to intermediate one
        //and operator is appended to it
        //for example, if intermediate = '(123+456)×789', operator = '÷' then we get '456831÷'
        //the result of calculation is guaranteed to fit in main display and intermediate display has more capacity
        if (state != State.ERROR) {
            Log.d("result", String.valueOf(result));
            boolean prevAddParen = false;
            // this is not the first operator
            if (state == State.OPERATOR) {
                prevAddParen = addParen;
                intermediateDisplay.setText(intermediateDisplay.getText().subSequence(0, intermediateLength - 1));
                --intermediateLength;
            }
            //check whether we need to add parentheses to enforce correctness of the expression, e.g. (1+23)×45
            if (state != State.OPERATOR) {
                lowPriority = (pendingOperator != null && determineOperator(pendingOperator).precedence == 2);
            }
            addParen = lowPriority && (operator.precedence == 1);
            //handle the situation when previous operator had high priority (and required parentheses) and the current one has low priority
            if (state == State.OPERATOR && prevAddParen && !addParen) {
                intermediateDisplay.setText(intermediateDisplay.getText().subSequence(1, intermediateLength - 1));
            }
            //previous number probably should be trimmed
            if (state == State.NUMBER) {
                updateDisplay();
            }
            //add operator and right operand
            int additionLength = 1 + numberBuilder.length();
            additionLength += addParen ? 2 : 0;
            boolean shouldShorten = (intermediateLength + additionLength > MAX_INTERMEDIATE_LENGTH);
            Log.d("shouldShorten ", String.valueOf(shouldShorten));
            if (!shouldShorten && state != State.OPERATOR) {
                intermediateDisplay.append(mainDisplay.getText());
            }
            //operation that should be completed before the current one
            Log.d("pendingOperator", pendingOperator == null ? "null" : pendingOperator.toString());
            if (pendingOperator != null) {
                if (state == State.OPERATOR) {
                    pendingOperator = null;
                } else {
                    rightOperand = readNumber();
                    calculate();
                }
            } else if (state == State.NUMBER) {
                // if numberBuilder was emptied by last call to reset()
                if (numberBuilder.length() != 0)
                    result = readNumber();
            }
            if (state == State.ERROR) {
                return;
            }
            pendingOperator = operatorRepr;
            if (shouldShorten) {
                intermediateDisplay.setText(mainDisplay.getText());
            }
            if (!shouldShorten && addParen && (state != State.OPERATOR || !prevAddParen)) {
                intermediateDisplay.setText("(" + intermediateDisplay.getText().toString() + ")");
            }
            state = State.OPERATOR;
            intermediateDisplay.append(String.valueOf(operatorRepr));
            isFractional = false;
            updateNumber();
        }
    }

    public void onNegate(View view) {
        if (state != State.ERROR) {
            // user could change sign of the current number at any time
            Log.d("state", state.toString());
            Log.d("result", result.toString());
            if (state != State.NUMBER && state != State.FINAL_RESULT) {
                return;
            }
            if (state == State.NUMBER) {
                if (numberBuilder.toString().equals("0"))
                    return;
                String previous = numberBuilder.toString();
                numberBuilder.setLength(0);
                if (previous.charAt(0) != '-')
                    numberBuilder.append("-").append(previous);
                else
                    numberBuilder.append(previous.substring(1));
                mainDisplay.setText(numberBuilder);

            } else if (state == State.FINAL_RESULT) {
                result = -result;
                mainDisplay.setText(trimNumber(String.valueOf(result)));
            }
        }
    }

    public void onClear(View view) {
        mainDisplay.setText(initialValue);
        intermediateDisplay.setText("");
        reset();
        result = 0.;
        state = State.NONE;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);
        mainDisplay = (TextView) findViewById(R.id.result);
        intermediateDisplay = (TextView) findViewById(R.id.intermediate);
        decimalSeparator = getString(R.string.decimal_separator).charAt(0);
        initialValue = getString(R.string.initial_value);
        errorMessage = getString(R.string.error_message);
        numberBuilder = new StringBuilder();
        reset();
        result = 0.;
        state = State.NONE;
        if (savedInstanceState != null) {
            lowPriority = savedInstanceState.getBoolean(LOW_PRIORITY);
            isFractional = savedInstanceState.getBoolean(IS_FRACTIONAL);
            addParen = savedInstanceState.getBoolean(ADD_PAREN);
            state = (State) savedInstanceState.getSerializable(STATE);
            result = savedInstanceState.getDouble(RESULT);
            pendingOperator = savedInstanceState.getChar(PENDING_OPERATOR);
            rightOperand = savedInstanceState.getDouble(RIGHT_OPERAND);
            mainDisplay.setText(savedInstanceState.getString(MAIN_TEXT));
            intermediateDisplay.setText(savedInstanceState.getString(INTERMEDIATE_TEXT));
            numberBuilder = new StringBuilder(savedInstanceState.getString(NUMBER_BUILDER));
            Log.d("onCreate Result ", String.valueOf(result));
            Log.d("onCreate Number ", numberBuilder.toString());
            Log.d("onCreate isFractional ", String.valueOf(isFractional));
            Log.d("onCreate lowPriority ", String.valueOf(lowPriority));
            Log.d("onCreate state", state.toString());
        }
    }
}
