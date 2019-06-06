import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Node
{
    int pos;
    Node parent;
    double val;

    public Node(int pos) {
        this.pos = pos;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public int getPos() {
        return pos;
    }

    public double getVal() {
        return val;
    }

    public void setVal(double val) {
        this.val = val;
    }
}

abstract class Operation extends Node
{
    Node left, right;

    public Operation(int pos) {
        super(pos);
    }

    public void setLeft(Node left) {
        left.setParent(this);
        this.left = left;
    }

    public void setRight(Node right) {
        right.setParent(this);
        this.right = right;
    }

    public abstract double operate(Node left, Node right);

    public abstract int getPriority();

    @Override
    public double getVal() {
        return operate(left, right);
    }

    public Operation getParent() {
        return (Operation) parent;
    }
}

class SumOperator extends Operation
{
    static final int priority = 1;
    public SumOperator(int pos) {
        super(pos);
    }

    @Override
    public double operate(Node left, Node right) {
        return left.getVal() + right.getVal();
    }

    @Override
    public int getPriority() {
        return priority;
    }
}

class MulOperator extends Operation
{
    static final int priority = 2;

    public MulOperator(int pos) {
        super(pos);
    }

    @Override
    public double operate(Node left, Node right) {
        return left.getVal() * right.getVal();
    }

    @Override
    public int getPriority() {
        return priority;
    }
}

class NodeFactory
{
    enum Token
    {
        TOKEN_SUM(Pattern.compile("(\\+)")),
        TOKEN_MUL(Pattern.compile("(\\*)")),
        TOKEN_OP(Pattern.compile("([0-9]+)"))
        ;

        Pattern pattern;
        Token(Pattern pattern)
        {
            this.pattern = pattern;
        }

        public Matcher match(String lexem)
        {
            return pattern.matcher(lexem);
        }
    }

    public static Node fromToken(String lexem)
    {
        Node node = null;
        for( Token token : Token.values() ) {
            Matcher matcher = token.match(lexem);
            if( matcher.matches()) {
                System.out.println(token);
                switch (token) {
                    case TOKEN_OP:
                        node = new Node(matcher.regionStart());
                        node.setVal(Double.parseDouble(matcher.group()));
                        break;
                    case TOKEN_MUL:
                        node = new MulOperator(matcher.regionStart());
                        break;
                    case TOKEN_SUM:
                        node = new SumOperator(matcher.regionStart());
                        break;
                }
            }
        }
        return node;
    }
}

public class Calculator {

    static List<Node> nodes = new ArrayList<>();
    static Stack<Node> operands = new Stack<>();
    static ArrayList<Operation> operations = new ArrayList<>();
    static Operation operationPtr;

    public static void main(String[] args)
    {
        StringBuffer buffer = new StringBuffer();
        char[] input = "1+2+3+4+5+6+8*3*4*3+2+3+4+5*3*4".toCharArray();

        for( int i = 0; i < input.length; i++ ) {
            buffer.append(input[i]);
            Node node = NodeFactory.fromToken(buffer.toString());
            if ( node == null) throw new SyntaxException("+, * and [0-9] are allowed only");
            if(node instanceof Operation) {
                operationPtr = (Operation) node;
            } else {
                operands.push(node);
            }

            if( operands.size() == 2 ) {
                operationPtr.setRight(operands.pop());
                operationPtr.setLeft(operands.pop());
                operands.push(operationPtr.right);
                operations.add(operationPtr);
            }

            buffer = new StringBuffer();
        }

        Operation second = null;

        for(int i = 1; i < operations.size(); i ++) {
            Operation first = (Operation) operations.get(i - 1);
            second = (Operation) operations.get(i);
            if(  second.getPriority() >= first.getPriority() ) {
                first.setRight(second);
            } else if (second.getPriority() < first.getPriority()) {
                while (first != null) {
                    second.parent = first.parent;
                    second.left = first;
                    first.parent = second;
                    first = (Operation) second.parent;
                }
            }
        }

        while (second.getParent() != null) {
            second = (Operation) second.getParent();
        }

        System.out.println(second.getVal());
    }
}
