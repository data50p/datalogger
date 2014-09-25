/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.parser;

import datalogger.server.parser.CalculatorParser3.CalcNode;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.support.Var;
import org.parboiled.trees.ImmutableBinaryTreeNode;

/**
 * A calculator parser building an AST representing the expression structure before performing the actual calculation. The value field of the parse tree nodes
 * is used for AST nodes. As opposed to the CalculatorParser2 this parser also supports floating point operations, negative numbers, a "power" and a "SQRT"
 * operation as well as optional whitespace between the various expressions components.
 */
@BuildParseTree
public class CalculatorParser3 extends CalculatorParser<CalcNode> {

    @Override
    public Rule InputLine() {
	return Sequence(Expression(), EOI);
    }

    Rule Expression() {
	Var<String> op = new Var<String>();
	return Sequence(
		Term(),
		ZeroOrMore(
			// we use a FirstOf(String, String) instead of a AnyOf(String) so we can use the
			// fromStringLiteral transformation (see below), which automatically consumes trailing whitespace
			FirstOf("+ ", "- "), op.set(""+matchedChar()),
			Term(),
			// same as in CalculatorParser2
			push(new CalcNode(op.get(), pop(1), pop()))
		)
	);
    }

    Rule Term() {
	Var<String> op = new Var<String>();
	return Sequence(
		Factor(),
		ZeroOrMore(
			FirstOf("* ", "/ "), op.set(""+matchedChar()),
			Factor(),
			push(new CalcNode(op.get(), pop(1), pop()))
		)
	);
    }

    Rule Factor() {
	return Sequence(
		Atom(),
		ZeroOrMore(
			"^ ",
			Atom(),
			push(new CalcNode("^", pop(1), pop()))
		)
	);
    }

    Rule Atom() {
	return FirstOf(Number(), Fun(), SquareRoot(), Parens());
    }

    Rule SquareRoot() {
	return Sequence(
		"SQRT ",
		Parens(),
		// create a new AST node with a special operator 'R' and only one child
		push(new CalcNode("R", pop(), null))
	);
    }

    Rule Fun() {
	return Sequence(
		"FUN ",
		Parens(),
		// create a new AST node with a special operator 'R' and only one child
		push(new CalcNode("fun", pop(), null))
	);
    }

    Rule Parens() {
	return Sequence("( ", Expression(), ") ");
    }

    Rule Number() {
	return Sequence(
		// we use another Sequence in the "Number" Sequence so we can easily access the input text matched
		// by the three enclosed rules with "match()" or "matchOrDefault()"
		Sequence(
			Optional('-'),
			OneOrMore(Digit()),
			Optional('.', OneOrMore(Digit()))
		),
		// the matchOrDefault() call returns the matched input text of the immediately preceding rule
		// or a default string (in this case if it is run during error recovery (resynchronization))
		push(new CalcNode(Double.parseDouble(matchOrDefault("0")))),
		WhiteSpace()
	);
    }

    Rule Digit() {
	return CharRange('0', '9');
    }

    Rule WhiteSpace() {
	return ZeroOrMore(AnyOf(" \t\f"));
    }

    // we redefine the rule creation for string literals to automatically match trailing whitespace if the string
    // literal ends with a space character, this way we don't have to insert extra whitespace() rules after each
    // character or string literal
    @Override
    protected Rule fromStringLiteral(String string) {
	return string.endsWith(" ")
		? Sequence(String(string.substring(0, string.length() - 1)), WhiteSpace())
		: String(string);
    }

    //****************************************************************
    /**
     * The AST node for the calculators. The type of the node is carried as a String that can either contain an operator char or be null. In the latter case the
     * AST node is a leaf directly containing a value.
     */
    public static class CalcNode extends ImmutableBinaryTreeNode<CalcNode> {

	private double value;
	private String operator;

	public CalcNode(double value) {
	    super(null, null);
	    this.value = value;
	}

	public CalcNode(String operator, CalcNode left, CalcNode right) {
	    super(left, right);
	    this.operator = operator;
	}

	public double getValue() {
	    if (operator == null) {
		return value;
	    }
	    if (operator.length() == 1) {

		Character op = operator.charAt(0);
		switch (op) {
		    case '+':
			return left().getValue() + right().getValue();
		    case '-':
			return left().getValue() - right().getValue();
		    case '*':
			return left().getValue() * right().getValue();
		    case '/':
			return left().getValue() / right().getValue();
		    case '^':
			return Math.pow(left().getValue(), right().getValue());
		    case 'R':
			return Math.sqrt(left().getValue());
		    default:
			throw new IllegalStateException();
		}

	    } else {

		if (operator.equals("fun")) {
		    return left().getValue() * 1001;
		} else {
		    throw new IllegalStateException();
		}

	    }
	}

	@Override
	public String toString() {
	    return (operator == null ? "Value " + value : "Operator '" + operator + '\'') + " | " + getValue();
	}
    }

    //**************** MAIN ****************
    public static void main(String[] args) {
	main(CalculatorParser3.class);
    }
}
