package edu.zohora.toylanguage;

import edu.zohora.toylanguage.interpreter.Parser;
import edu.zohora.toylanguage.interpreter.Tokenizer;
import edu.zohora.toylanguage.interpreter.TreeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Application {
    public static void main(String[] args) {
        try {
            String p1 = "x = 3; y = 2; y = x * (5 - y);";

            for (Map.Entry<String,Integer> entry : getVarsTable(p1).entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static Map<String, Integer> getVarsTable(String program) throws Exception {
        Parser parser = new Parser();
        List<String> tokens = Tokenizer.tokenize(program);
        return buildVarsTable(parser.parse(tokens));
    }

    private static Map<String, Integer> buildVarsTable(List<TreeNode> expressionList) throws Exception {
        Map<String, Integer> varsTable = new HashMap<>();
        for (TreeNode node : expressionList) {
            String var = node.getLeft().getValue();
            int value = evaluateAST(node.getRight(), varsTable);
            varsTable.put(var, value);
        }
        return varsTable;
    }

    private static int evaluateAST(TreeNode node, Map<String, Integer> varsTable) throws Exception {
        if (node == null) return 1;
        int leftVal = evaluateAST(node.getLeft(), varsTable);
        int rightVal = evaluateAST(node.getRight(), varsTable);

        String value = node.getValue();

        switch (value) {
            case "+":
                return leftVal + rightVal;
            case "-":
                return leftVal - rightVal;
            case "*":
                return leftVal * rightVal;
            default:
                if (Pattern.matches("^(0|-?[1-9][0-9]*)$", value)) return Integer.parseInt(value);
                else if (Pattern.matches("^([a-zA-Z_][a-zA-Z_0-9]*)$", value)) return varsTable.get(value);
                throw new Exception(String.format("Error: '%s' may have not been initialized", value));
        }
    }
}
