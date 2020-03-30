package edu.zohora.toylanguage.interpreter;

import java.util.*;
import java.util.regex.Pattern;

public class Parser {
    private int cursor;
    private List<String> tokens;
    private Set<String> initializedVars;
    // Needed to construct the AST using Shunting-yard algorithm
    private Stack<String> operatorStack;
    private Stack<TreeNode> exprStack;

    private final String IDENTIFIER = "([a-zA-Z_][a-zA-Z_0-9]*)";
    private final String NUMBERS = "(0|-?[1-9][0-9]*)";
    private final String OPERATORS = "([*+-=])";

    public List<TreeNode> parse(List<String> tokens) throws Exception {
        this.initializedVars = new HashSet<>();
        this.operatorStack = new Stack<>();
        this.exprStack = new Stack<>();
        this.tokens = tokens;
        this.cursor = 0;

        try {
            List<TreeNode> expressionList = program();
            if (expressionList != null) return expressionList;
            throw new Exception("Error: Could not parse the tree");
        } catch (IndexOutOfBoundsException e) {
            throw new Exception("Error: ';' expected");
        }
    }

    private List<TreeNode> program() throws Exception {
        List<TreeNode> expressionList = new ArrayList<>();
        while (this.cursor < this.tokens.size()) {
            TreeNode A = assignment();
            if (A != null) {
                expressionList.add(A);
            } else {
                return null;
            }
        }

        return expressionList;
    }

    private TreeNode assignment() throws Exception {
        String id = identifier();
        if (id != null) {
            this.cursor++;
            if (this.tokens.get(cursor).compareTo("=") == 0) {
                this.cursor++;
                if (expression()) {
                    // check for closing semi-colon
                    if (this.tokens.get(cursor).compareTo(";") == 0) {
                        emptyStacks();
                        initializedVars.add(id);
                        TreeNode E = exprStack.pop();
                        TreeNode I = new TreeNode(id);
                        TreeNode A = new TreeNode("=", I, E);
                        this.cursor++;
                        return A;
                    } else {
                        throw new Exception("Error: ';' expected");
                    }
                } else {
                    return null;
                }
            } else {
                throw new Exception("Error: '=' expected");
            }
        } else {
            throw new Exception("Error: Illegal start of expression");
        }
    }

    private String identifier() throws IndexOutOfBoundsException {
        if (Pattern.matches(IDENTIFIER, this.tokens.get(cursor))) {
            return this.tokens.get(cursor);
        }
        return null;
    }

    private boolean expression() throws Exception {
        if (term()) {
            return expressionPrime();
        } else {
            throw new Exception("Error: Illegal start of expression");
        }
    }

    private boolean expressionPrime() throws Exception {
        if (Pattern.matches("^[+-]$", this.tokens.get(cursor))) {
            String op = this.tokens.get(cursor);
            updateStacks(op);
            this.cursor++;

            if (term()) {
                return expressionPrime();
            } else {
                throw new Exception("Error: Illegal start of expression");
            }
        }
        return true;
    }

    private boolean term() throws Exception {
        if (factor()) {
            return termPrime();
        } else {
            throw new Exception("Error: Illegal start of expression");
        }
    }

    private boolean termPrime() throws Exception {
        if (this.tokens.get(cursor).compareTo("*") == 0) {
            // add * to tree
            String op = this.tokens.get(cursor);
            updateStacks(op);
            this.cursor++;
            if (factor()) {
                return termPrime();
            } else {
                throw new Exception("Error: Illegal start of expression");
            }
        }
        return true;
    }

    private boolean factor() throws Exception {
        if (this.tokens.get(cursor).compareTo("(") == 0) {
            updateStacks("(");
            this.cursor++;
            // check if '(' is followed by valid Expression
            if (expression()) {
                // look for closing ')'
                if (this.tokens.get(cursor).compareTo(")") == 0) {
                    updateStacks(")");
                    this.cursor++;
                } else {
                    throw new Exception("Error: ')' expected");
                }
            } else {
                throw new Exception("Error: Illegal start of expression");
            }
        } else if (Pattern.matches("^[+-]$", this.tokens.get(cursor))) {
            // add + or - to tree
            String op = this.tokens.get(cursor);
            updateStacks(op);
            this.cursor++;
            return factor();
        } else if (Pattern.matches("^" + NUMBERS + "$", this.tokens.get(cursor))) {
            String d = this.tokens.get(cursor);
            updateStacks(d);
            this.cursor++;
        } else {
            String id = identifier();
            if (id != null && initializedVars.contains(id)) {
                updateStacks(id);
                this.cursor++;
                return true;
            } else if (id != null){
                throw new Exception("Error: '%s' may have not been initialized");
            } else {
                throw new Exception("Error: Illegal start of expression");
            }
        }
        return true;
    }

    private void emptyStacks() {
        while (!operatorStack.isEmpty()) {
            String operator = operatorStack.pop();
            // The second operand was pushed last.
            TreeNode n2 = exprStack.isEmpty() ? null : exprStack.pop();
            TreeNode n1 = exprStack.isEmpty() ? null : exprStack.pop();
            exprStack.push(new TreeNode(operator, n1, n2));
        }
    }

    // Uses Shunting-yard algorithm to construct a Abstract Syntax Tree
    // https://en.wikipedia.org/wiki/Shunting-yard_algorithm
    private void updateStacks(String token) {
        if (Pattern.matches("^" + NUMBERS + "|" + IDENTIFIER + "$", token)) {
            exprStack.add(new TreeNode(token));
        }
        else if (token.compareTo("(") == 0) {
            operatorStack.push(token);
        }
        else if (Pattern.matches(OPERATORS, token)) {
            while (!operatorStack.isEmpty() && opPrecedence(operatorStack.peek()) >= opPrecedence(token)) {
                String operator = operatorStack.pop();
                // The second operand was pushed last
                TreeNode n2 = exprStack.isEmpty() ? null : exprStack.pop();
                TreeNode n1 = exprStack.isEmpty() ? null : exprStack.pop();
                exprStack.push(new TreeNode(operator, n1, n2));
            }
            // push operator onto stack
            operatorStack.push(token);
        }
        else if (token.compareTo(")") == 0) {
            while (!operatorStack.isEmpty() && operatorStack.peek().compareTo("(") != 0) {
                String operator = operatorStack.pop();
                // The second operand was pushed last.
                TreeNode n2 = exprStack.isEmpty() ? null : exprStack.pop();
                TreeNode n1 = exprStack.isEmpty() ? null : exprStack.pop();
                exprStack.push(new TreeNode(operator, n1, n2));
            }
            // Pop the '(' off the operator stack.
            if (!operatorStack.isEmpty()) operatorStack.pop();
        }
    }

    private int opPrecedence(String op) throws IllegalArgumentException {
        switch (op) {
            case "(": case ")":
                return 0;
            case "+": case "-":
                return 1;
            case "*":
                return 2;
            default:
                throw new IllegalArgumentException(String.format("Operator unknown: %s", op));
        }
    }
}
